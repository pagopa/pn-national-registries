package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_INAD_RESPONSE_EMPTY;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfRequestedMetric;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetricFromBatchRequest;

@Component
@Slf4j
@RequiredArgsConstructor
public class InadPollingService {

    private final InadClient inadClient;
    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final InadConverter inadConverter;
    private final GatewayUtils gatewayUtils;
    private final PollingService pollingService;

    public Mono<Void> handleMessage(PnPollingEvent.Payload payload) {
        return callInadStatusEService(payload.getBatchId(), payload.getPollingId())
                .onErrorResume(e -> incrementAndCheckRetry(payload.getBatchId(), payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, e)
                        .then(Mono.empty()))
                .flatMap(response -> handlePollingStatus(payload, response));
    }

    private Mono<ResponseStatusListDigitalAddress> callInadStatusEService(String batchId, String pollingId) {
        return inadClient.checkMassiveRequestStatus(pollingId)
                .doOnNext(response -> log.info("INAD - batchId {} - pollingId {} - polling state: {}", batchId, pollingId, response == null ? null : response.getState()))
                .doOnError(e -> log.warn("INAD - batchId {} - pollingId {} - failed to check polling status", batchId, pollingId, e));
    }

    private Mono<Void> handlePollingStatus(PnPollingEvent.Payload payload, ResponseStatusListDigitalAddress response) {
        if (Objects.isNull(response) || Objects.isNull(response.getState())) {
            return Mono.error(new PnInternalException(String.format("INAD - batchId %s - empty polling response", payload.getBatchId()), ERROR_CODE_NATIONAL_REGISTRIES_INAD_RESPONSE_EMPTY));
        }
        StatusProcessingRequest state = response.getState();
        return switch (state) {
            case PRESA_IN_CARICO, IN_ELABORAZIONE -> incrementAndCheckRetry(
                    payload.getBatchId(),
                    payload.getErrorRetryCount(),
                    payload.getInProgressRetryCount(),
                    true,
                    null
            );
            case DISPONIBILE -> handleAvailableResult(payload);
        };
    }

    private Mono<Void> handleAvailableResult(PnPollingEvent.Payload payload) {
        return callInadResultEService(payload.getBatchId(), payload.getPollingId())
                .onErrorResume(e -> incrementAndCheckRetry(payload.getBatchId(), payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, e)
                        .then(Mono.empty()))
                .flatMap(response -> handleSuccessfulPolling(payload.getBatchId(), payload.getBatchSize(), response));

    }

    private Mono<ResponseListDigitalAddress> callInadResultEService(String batchId, String pollingId) {
        return inadClient.getMassiveRequestResult(pollingId)
                .doOnNext(unused -> log.info("INAD - batchId {} - pollingId {} - retrieved results", batchId, pollingId))
                .doOnError(e -> log.warn("INAD - batchId {} - pollingId {} - failed to retrieve massive result", batchId, pollingId, e));
    }

    private Mono<Void> incrementAndCheckRetry(String batchId, Integer retryCount, Integer inProgressRetryCount, boolean isInProgressRetry, Throwable throwable) {
        var inadConfig = nationalRegistriesConfig.getInad();
        return pollingService.incrementAndCheckRetry(batchId, retryCount, inProgressRetryCount, isInProgressRetry, inadConfig.getBatchPollingMaxRetry(), inadConfig.getBatchPollingInProgressMaxRetry(), GatewayDownstreamService.INAD, throwable);
    }

    private Mono<Void> handleSuccessfulPolling(String batchId, Integer batchSize, ResponseListDigitalAddress response) {
        logCfRequestedMetric(batchId, GatewayDownstreamService.INAD, batchSize);
        Map<String, ResponseRequestDigitalAddress> resultByTaxId = inadConverter.mapResponseByTaxId(response);
        return Flux.from(nationalRegistriesRequestsRepository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .flatMapIterable(Page::items)
                .flatMap(request -> processSuccessfulRequest(request, resultByTaxId))
                .then(pollingService.markBatchAsWorked(batchId, GatewayDownstreamService.INAD))
                .then();
    }

    private Mono<Void> processSuccessfulRequest(NationalRegistriesRequest request, Map<String, ResponseRequestDigitalAddress> resultByTaxId) {
        ResponseRequestDigitalAddress inadResponse = resultByTaxId.get(request.getTaxId());
        if (Objects.isNull(inadResponse)) {
            log.warn("INAD - correlationId {} - taxId not present in massive response", request.getCorrelationId());
            return handlePecNotFoundResponse(request,  GatewayConverter.inadToSqsDto(request.getCorrelationId(), null));
        }
        return processInadResponse(request, inadResponse);
    }

    private Mono<Void> processInadResponse(NationalRegistriesRequest request, ResponseRequestDigitalAddress inadResponse) {
        RecipientType recipientType = retrieveRecipientType(request.getTaxId(), request.getRecipientType());
        String correlationId = request.getCorrelationId();

        return Mono.fromCallable(() -> InadConverter.mapToResponseOk(inadResponse, recipientType, request.getTaxId(), nationalRegistriesConfig.isEnablePfPecFallbackFlow(), correlationId))
                .doOnNext(unused -> log.info("INAD - retrieved digital address from massive response for correlationId: {}", correlationId))
                .map(response -> GatewayConverter.inadToSqsDto(correlationId, response))
                .flatMap(codeSqsDto -> {
                    if (!CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())) {
                        logCfWithAddressMetricFromBatchRequest(
                                codeSqsDto,
                                request.getCorrelationId(),
                                request.getTaxId(),
                                request.getRecipientType(),
                                GatewayDownstreamService.INAD
                        );
                        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
                        return pollingService.markRequestAsWorked(request, GatewayDownstreamService.INAD, resultPayload, true);
                    }
                    return handlePecNotFoundResponse(request, codeSqsDto);
                })
                .onErrorResume(e -> {
                    CodeSqsDto codeSqsDto = GatewayConverter.errorInadToSqsDto(correlationId, e);
                    if (codeSqsDto != null) {
                        log.info("correlationId: {} - INAD - indirizzo non presente", correlationId);
                        return handlePecNotFoundResponse(request, codeSqsDto);
                    }
                    return Mono.error(e);
                });
    }

    private Mono<Void> handlePecNotFoundResponse(NationalRegistriesRequest request, CodeSqsDto codeSqsDto) {
        if (nationalRegistriesConfig.isEnablePfPecFallbackFlow() && PF.name().equalsIgnoreCase(request.getRecipientType())) {
            return pollingService.createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC);
        }
        String resultPayload = gatewayUtils.convertCodeSqsDtoToString(codeSqsDto);
        return pollingService.markRequestAsWorked(request, GatewayDownstreamService.INAD, resultPayload, true);
    }
}