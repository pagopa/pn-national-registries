package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.converter.GatewayConverter.errorInadToSqsDto;
import static it.pagopa.pn.national.registries.converter.GatewayConverter.inadToSqsDto;
import static it.pagopa.pn.national.registries.entity.NationalRegistriesBatch.SK;
import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.removeInvalidEmails;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;
import static it.pagopa.pn.national.registries.utils.MetricUtils.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class InipecPollingService {

    private final InfoCamereClient infoCamereClient;
    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;
    private final InfoCamereConverter infoCamereConverter;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final InadService inadService;
    private final GatewayUtils gatewayUtils;

    public Mono<Void> handleMessage(PnPollingEvent.Payload payload) {
        return callIniPecEService(payload.getBatchId(), payload.getPollingId())
                .onErrorResume(PnNationalRegistriesException.class, e -> incrementAndCheckRetry(payload.getBatchId(), payload.getRetryCount(), payload.getInProgressRetryCount(), e)
                        .then(Mono.empty()))
                .flatMap(response -> handleSuccessfulPolling(payload.getBatchId(), payload.getBatchSize(), response));
    }

    private Mono<Void> incrementAndCheckRetry(String batchId, Integer retryCount, Integer inProgressRetryCount, Throwable throwable) {
        int retry = Optional.ofNullable(retryCount).orElse(0);
        int inProgressRetry = Optional.ofNullable(inProgressRetryCount).orElse(0);
        boolean isInProgressRetry = infoCamereConverter.isPollingResponseNotReady(throwable);
        int retryToIncrement = isInProgressRetry ? inProgressRetry + 1 : retry + 1;

        var iniPecConfig = nationalRegistriesConfig.getInipec();
        boolean retryExhausted = isInProgressRetry ? retryToIncrement >= iniPecConfig.getBatchPollingInProgressMaxRetry() : retryToIncrement >= iniPecConfig.getBatchPollingMaxRetry();
        boolean badRequest = throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST;

        if (retryExhausted || badRequest) {
            return handlePollingFailure(batchId, throwable);
        }

        return nationalRegistriesBatchesRepository.resetStatusForRetry(batchId, SK, retryToIncrement, isInProgressRetry, GatewayDownstreamService.INIPEC)
                .doOnNext(batch -> log.debug("IniPEC - batchId {} - retry updated to {} (inProgress: {})", batchId, retryToIncrement, isInProgressRetry))
                .doOnError(error -> log.warn("IniPEC - batchId {} - failed to update retry", batchId, error))
                .then();
    }

    private Mono<Void> handlePollingFailure(String batchId, Throwable throwable) {
        log.warn("IniPEC - batchId {} - polling retries exhausted or non-retryable error", batchId, throwable);
        return updateBatchRequestToError(batchId)
                .then(nationalRegistriesBatchesRepository.updateToFinalStatus(batchId, SK, BatchesStatusEnum.ERROR))
                .then();
    }

    private Mono<Void> updateBatchRequestToError(String batchId) {
        return Flux.from(nationalRegistriesRequestsRepository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .flatMapIterable(Page::items)
                .flatMap(request -> nationalRegistriesRequestsRepository.updateStatusToError(request.getCorrelationId(), request.getCreatedAt().toString(), request.getRegistry()))
                .then();
    }

    private Mono<Void> handleSuccessfulPolling(String batchId, Integer batchSize, IniPecPollingResponse response) {
        logInipecCfRequestedMetric(batchId, GatewayDownstreamService.INIPEC, batchSize);
        return Flux.from(nationalRegistriesRequestsRepository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .flatMapIterable(Page::items)
                .flatMap(request -> processSuccessfulRequest(request, response))
                .then(nationalRegistriesBatchesRepository.updateToFinalStatus(batchId, SK, BatchesStatusEnum.WORKED))
                .doOnSuccess(batch -> log.debug("IniPEC - batchId {} - updated status to WORKED", batchId))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to update status to WORKED", batchId, e))
                .then();
    }

    private Mono<Void> processSuccessfulRequest(NationalRegistriesRequest request, IniPecPollingResponse response) {
        Pec pec = infoCamereConverter.retrieveBatchRequestPec(request.getTaxId(), response);
        if (Objects.isNull(pec)) {
            return handlePecNotFoundResponse(request);
        }
        return evaluateStatoImpresa(request, pec);
    }

    private Mono<IniPecPollingResponse> callIniPecEService(String batchId, String pollingId) {
        return infoCamereClient.callEServiceRequestPec(pollingId)
                .doOnNext(response -> {
                    if (infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
                        throw new PnNationalRegistriesException(response.getDescription(), HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(), null, null,
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    } else {
                        log.info("IniPEC - batchId {} - pollingId {} - response pec size: {}", batchId, pollingId, response.getElencoPec().size());
                    }
                })
                .doOnError(e -> log.warn("IniPEC - pollingId {} - failed to call EService", pollingId, e));
    }



    private Mono<Void> callInadEservice(NationalRegistriesRequest request) {
        RecipientType recipientType = retrieveRecipientType(request.getTaxId(), request.getRecipientType());
        String correlationId = request.getCorrelationId().split(nationalRegistriesConfig.getInipec().getBatchRequestPkSeparator())[0];
        if (nationalRegistriesConfig.isEnableInadMassiveApi()) {
            return callInadMassive(request);
        }
        return callInadSync(request, recipientType, correlationId);
    }

    private Mono<Void> callInadMassive(NationalRegistriesRequest request) {
        NationalRegistriesRequest nationalRegistriesRequest = new NationalRegistriesRequest(request, RequestStatusEnum.NOT_WORKED, GatewayDownstreamService.INAD, null);
        return nationalRegistriesRequestsRepository.putRequest(nationalRegistriesRequest)
                .doOnNext(r -> log.info("Created request for correlationId: {} for registry INAD", r.getCorrelationId()))
                .then(markInipecRequestAsWorked(request));
    }

    private Mono<Void> markInipecRequestAsWorked(NationalRegistriesRequest request) {
        return nationalRegistriesRequestsRepository.updateStatusToWorked(request.getCorrelationId(), request.getCreatedAt().toString(), null, GatewayDownstreamService.INIPEC, false).then();
    }

    private Mono<Void> callInadSync(NationalRegistriesRequest request, RecipientType recipientType, String correlationId) {
        return inadService.callEService(GatewayConverter.convertToGetDigitalAddressInadRequest(request.getCorrelationId(), request.getTaxId()), recipientType, correlationId)
                .doOnNext(response -> logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1))
                .flatMap(DigitalAddressUtils::emailValidation)
                .onErrorResume(e -> handleInadError(request, correlationId, e)
                        .then(Mono.empty()))
                .flatMap(response -> handleInadResponse(request, correlationId, response))
                .doOnSuccess(r -> log.info("Retrieved digital address from INAD for correlationId: {}", request.getCorrelationId()))
                .then();
    }


    private Mono<Void> handleInadResponse(NationalRegistriesRequest request, String correlationId, GetDigitalAddressINADOKDto response) {
        CodeSqsDto codeSqsDto = inadToSqsDto(correlationId, response);
        logCfWithAddressMetricFromBatchRequest(codeSqsDto, request.getCorrelationId(), request.getTaxId(), request.getRecipientType(), GatewayDownstreamService.INAD);
        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.WORKED, GatewayDownstreamService.INAD, resultPayload);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest)
                .then(markInipecRequestAsWorked(request));
    }

    private Mono<Void> handleInadError(NationalRegistriesRequest request, String correlationId, Throwable throwable) {
        gatewayUtils.logEServiceError(throwable, "can not retrieve digital address from INAD: {}");
        CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, throwable);
        if (Objects.isNull(codeSqsDto)) {
            return createInadErrorRequest(request).then(markInipecRequestAsWorked(request));
        }
        logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1);
        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.WORKED, GatewayDownstreamService.INAD, resultPayload);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest)
                .then(markInipecRequestAsWorked(request));
    }

    private Mono<Void> createInadErrorRequest(NationalRegistriesRequest request) {
        NationalRegistriesRequest inadRequest = new NationalRegistriesRequest(request, RequestStatusEnum.ERROR, GatewayDownstreamService.INAD, null);
        return nationalRegistriesRequestsRepository.putRequest(inadRequest).then();
    }

    private Mono<Void> evaluateStatoImpresa(NationalRegistriesRequest request, Pec pec) {
        log.info("evaluateStatoImpresa for correlationId: {} with statoImpresa: {}", request.getCorrelationId(), pec.getStatoImpresa());
        if (Objects.isNull(pec.getStatoImpresa())) {
            CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(request.getCorrelationId(), pec);
            int foundPecCount = CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()) ? 0 : codeSqsDto.getDigitalAddress().size();
            removeInvalidEmails(codeSqsDto);
            int validPecCount = CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()) ? 0 : codeSqsDto.getDigitalAddress().size();
            log.info("IniPEC - correlationId {} - statoImpresa is null - pec found: {}, valid pec: {}", request.getCorrelationId(), foundPecCount, validPecCount);
            if (validPecCount == 0) {
                log.info("IniPEC - correlationId {} - no valid digital address found in IniPEC response, starting fallback", request.getCorrelationId());
                return handlePecNotFoundResponse(request);
            }
            logCfWithAddressMetricFromBatchRequest(codeSqsDto, request.getCorrelationId(), request.getTaxId(), request.getRecipientType(), GatewayDownstreamService.INIPEC);
            String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
            return nationalRegistriesRequestsRepository.updateStatusToWorked(request.getCorrelationId(), request.getCreatedAt().toString(), resultPayload, GatewayDownstreamService.INIPEC, true).then();
        }
        return switch (pec.getStatoImpresa()) {
            case ER -> handleERState(request);
            case ND, NF -> handlePecNotFoundResponse(request);
        };
    }

    private Mono<Void> handlePecNotFoundResponse(NationalRegistriesRequest request) {
        if (PF.name().equalsIgnoreCase(request.getRecipientType())) {
            CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(request.getCorrelationId(), null);
            String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
            return nationalRegistriesRequestsRepository.updateStatusToWorked(request.getCorrelationId(), request.getCreatedAt().toString(), resultPayload, GatewayDownstreamService.INIPEC, true).then();
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
        return nationalRegistriesRequestsRepository.resetStatusForRetry(request.getCorrelationId(), request.getCreatedAt().toString(), retry, GatewayDownstreamService.INIPEC)
                .doOnSuccess(r -> log.debug("IniPEC - correlationId {} - reset for retry ({}/{})", request.getCorrelationId(), retry, maxRetry))
                .doOnError(e -> log.warn("IniPEC - correlationId {} - failed to reset for retry", request.getCorrelationId(), e))
                .then();
    }


}
