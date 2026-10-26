package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.InipecEvaluationResult;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.converter.GatewayConverter.errorInadToSqsDto;
import static it.pagopa.pn.national.registries.converter.GatewayConverter.inadToSqsDto;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;
import static it.pagopa.pn.national.registries.utils.MetricUtils.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class InipecPollingService {

    private final InfoCamereService infoCamereService;
    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final InfoCamereConverter infoCamereConverter;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final InadService inadService;
    private final GatewayUtils gatewayUtils;
    private final PollingService pollingService;

    public Mono<Void> handleMessage(PnPollingEvent.Payload payload) {
        return infoCamereService.callIniPecEService(payload.getBatchId(), payload.getPollingId())
                .onErrorResume(e -> incrementAndCheckRetry(payload.getBatchId(), payload.getErrorRetryCount(), payload.getInProgressRetryCount(), e)
                        .then(Mono.empty()))
                .flatMap(response -> handleSuccessfulPolling(payload.getBatchId(), payload.getBatchSize(), response));
    }

    private Mono<Void> incrementAndCheckRetry(String batchId, Integer retryCount, Integer inProgressRetryCount, Throwable throwable) {
        boolean isInProgressRetry = infoCamereConverter.isPollingResponseNotReady(throwable);
        var iniPecConfig = nationalRegistriesConfig.getInipec();
        return pollingService.incrementAndCheckRetry(batchId, retryCount, inProgressRetryCount, isInProgressRetry, iniPecConfig.getBatchPollingMaxRetry(), iniPecConfig.getBatchPollingInProgressMaxRetry(), GatewayDownstreamService.INIPEC, throwable);
    }

    private Mono<Void> handleSuccessfulPolling(String batchId, Integer batchSize, IniPecPollingResponse response) {
        logInipecCfRequestedMetric(batchId, GatewayDownstreamService.INIPEC, batchSize);
        Map<String, Pec> pecByTaxId = infoCamereConverter.mapPecByTaxId(response);
        return Flux.from(nationalRegistriesRequestsRepository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .flatMapIterable(Page::items)
                .flatMap(request -> processSuccessfulRequest(request, pecByTaxId))
                .then(pollingService.markBatchAsWorked(batchId, GatewayDownstreamService.INIPEC));
    }

    private Mono<Void> processSuccessfulRequest(NationalRegistriesRequest request, Map<String, Pec> pecByTaxId) {
        Pec pec = infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId);
        if (Objects.isNull(pec)) {
            return handlePecNotFoundResponse(request);
        }
        return evaluateStatoImpresa(request, pec);
    }

    private Mono<Void> callInadEservice(NationalRegistriesRequest request) {
        RecipientType recipientType = retrieveRecipientType(request.getTaxId(), request.getRecipientType());
        String correlationId = request.getCorrelationId().split(nationalRegistriesConfig.getInipec().getBatchRequestPkSeparator())[0];
        if (nationalRegistriesConfig.isEnableInadMassiveApi()) {
            return pollingService.createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
        }
        return callInadSync(request, recipientType, correlationId);
    }

    private Mono<Void> callInadSync(NationalRegistriesRequest request, RecipientType recipientType, String correlationId) {
        return inadService.callEService(GatewayConverter.convertToGetDigitalAddressInadRequest(request.getCorrelationId(), request.getTaxId()), recipientType, correlationId)
                .doOnNext(unused -> logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1))
                .flatMap(DigitalAddressUtils::emailValidation)
                .onErrorResume(e -> handleInadError(request, correlationId, e)
                        .then(Mono.empty()))
                .flatMap(response -> handleInadResponse(request, correlationId, response))
                .doOnSuccess(unused -> log.info("Retrieved digital address from INAD for correlationId: {}", request.getCorrelationId()))
                .then();
    }


    private Mono<Void> handleInadResponse(NationalRegistriesRequest request, String correlationId, GetDigitalAddressINADOKDto response) {
        CodeSqsDto codeSqsDto = inadToSqsDto(correlationId, response);
        logCfWithAddressMetricFromBatchRequest(codeSqsDto, request.getCorrelationId(), request.getTaxId(), request.getRecipientType(), GatewayDownstreamService.INAD);
        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.WORKED, GatewayDownstreamService.INAD, resultPayload);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest)
                .then(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false));
    }

    private Mono<Void> handleInadError(NationalRegistriesRequest request, String correlationId, Throwable throwable) {
        gatewayUtils.logEServiceError(throwable, "can not retrieve digital address from INAD: {}");
        CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, throwable);
        if (Objects.isNull(codeSqsDto)) {
            return createInadErrorRequest(request)
                    .then(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false));
        }
        logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1);
        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.WORKED, GatewayDownstreamService.INAD, resultPayload);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest)
                .then(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false));
    }

    private Mono<Void> createInadErrorRequest(NationalRegistriesRequest request) {
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.ERROR, GatewayDownstreamService.INAD, null);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest).then();
    }

    private Mono<Void> evaluateStatoImpresa(NationalRegistriesRequest request, Pec pec) {
        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa(request.getCorrelationId(), request.getTaxId(), request.getRecipientType(), pec);
        return switch (result) {
            case InipecEvaluationResult.Found found -> {
                String resultPayload = gatewayUtils.convertCodeSqsDtoToString(found.codeSqsDto());
                yield pollingService.markRequestAsWorked(
                        request,
                        GatewayDownstreamService.INIPEC,
                        resultPayload,
                        true
                );
            }
            case InipecEvaluationResult.NotFound ignored -> handlePecNotFoundResponse(request);
            case InipecEvaluationResult.Retry ignored -> handleERState(request);
        };
    }

    private Mono<Void> handlePecNotFoundResponse(NationalRegistriesRequest request) {
        if (PF.name().equalsIgnoreCase(request.getRecipientType())) {
            CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(request.getCorrelationId(), null);
            String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
            return pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, resultPayload, true);
        }
        return callInadEservice(request);
    }

    private Mono<Void> handleERState(NationalRegistriesRequest request) {
        logCfWithErrorMetric(request.getCorrelationId(), GatewayDownstreamService.INIPEC, retrieveRecipientType(request.getTaxId(), request.getRecipientType()));
        int retry = Optional.ofNullable(request.getRetryCount()).orElse(0) + 1;
        int maxRetry = nationalRegistriesConfig.getInipec().getBatchRequestMaxRetry();
        if (retry >= maxRetry) {
            log.warn("IniPEC - correlationId {} - max retry reached ({}/{})", request.getCorrelationId(), retry, maxRetry);
            return nationalRegistriesRequestsRepository.updateStatusToError(request.getCorrelationId(), request.getCreatedAt().toString(), GatewayDownstreamService.INIPEC).then();
        }
        Instant attemptAfter = Instant.now().plusSeconds(nationalRegistriesConfig.getInipec().getRequestRetryAfter());
        return nationalRegistriesRequestsRepository.resetStatusForRetry(request.getCorrelationId(), request.getCreatedAt().toString(), retry, attemptAfter, GatewayDownstreamService.INIPEC)
                .doOnSuccess(r -> log.debug("IniPEC - correlationId {} - reset for retry ({}/{})", request.getCorrelationId(), retry, maxRetry))
                .doOnError(e -> log.warn("IniPEC - correlationId {} - failed to reset for retry", request.getCorrelationId(), e))
                .then();
    }


}
