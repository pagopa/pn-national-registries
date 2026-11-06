package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetDigitalAddressIniPecRequest;
import static it.pagopa.pn.national.registries.entity.NationalRegistriesBatch.SK;
import static it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService.INIPEC;

@Component
@Slf4j
@RequiredArgsConstructor
public class PollingService {

    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final InfoCamereService infoCamereService;

    public Mono<Void> incrementAndCheckRetry(String batchId, Integer errorRetryCount, Integer inProgressRetryCount, boolean inProgress, int maxErrorRetry, int maxInProgressRetry, GatewayDownstreamService registry, Throwable throwable) {
        int errorRetry = Optional.ofNullable(errorRetryCount).orElse(0);
        int inProgressRetry = Optional.ofNullable(inProgressRetryCount).orElse(0);
        int retryToIncrement = inProgress ? inProgressRetry + 1 : errorRetry + 1;
        int retryLimit = inProgress ? maxInProgressRetry : maxErrorRetry;
        boolean retryExhausted = retryToIncrement >= retryLimit;
        boolean badRequest = throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST;

        if (retryExhausted || badRequest) {
            return handlePollingFailure(batchId, registry, throwable);
        }

        return nationalRegistriesBatchesRepository
                .resetStatusForRetry(batchId, SK, retryToIncrement, inProgress, registry)
                .doOnNext(unused -> log.debug("{} - batchId {} - retry updated to {} (inProgress: {})", registry, batchId, retryToIncrement, inProgress))
                .doOnError(error -> log.warn("{} - batchId {} - failed to update retry", registry, batchId, error))
                .then();
    }

    private Mono<Void> handlePollingFailure(String batchId, GatewayDownstreamService registry, Throwable throwable) {
        log.warn("{} - batchId {} - polling retries exhausted or non-retryable error", registry, batchId, throwable);
        return Flux.from(nationalRegistriesRequestsRepository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .flatMapIterable(Page::items)
                .concatMap(this::moveRequestToError)
                .then(nationalRegistriesBatchesRepository.updateToFinalStatus(batchId, SK, BatchesStatusEnum.ERROR))
                .then();
    }

    public Mono<Void> createFallbackRequest(NationalRegistriesRequest request, GatewayDownstreamService sourceRegistry, GatewayDownstreamService targetRegistry) {
        if(nationalRegistriesConfig.isEnableOldInipecFlow() && INIPEC.equals(targetRegistry)) {
            log.info("Creating fallback request on legacy INIPEC flow for correlationId: {}", request.getCorrelationId());
            Date referenceRequestDate = Date.from(
                    Instant.parse(request.getReferenceRequestDate())
            );
            return infoCamereService.getIniPecDigitalAddress(request.getClientId(), convertToGetDigitalAddressIniPecRequest(request.getCorrelationId(), request.getTaxId()), referenceRequestDate, RecipientType.fromString(request.getRecipientType()))
                    .then(markRequestAsWorked(request, sourceRegistry, null,false))
                    .then();
        }
        NationalRegistriesRequest fallbackRequest = new NationalRegistriesRequest(request, RequestStatusEnum.NOT_WORKED, targetRegistry, null);
        return nationalRegistriesRequestsRepository
                .putRequest(fallbackRequest)
                .doOnNext(r -> log.info("Created request for correlationId: {} for registry {}", r.getCorrelationId(), targetRegistry))
                .then(markRequestAsWorked(request, sourceRegistry, null,false));
    }

    public Mono<Void> markRequestAsWorked(NationalRegistriesRequest request, GatewayDownstreamService registry, String resultPayload, boolean publishResult) {
        return nationalRegistriesRequestsRepository
                .updateStatusToWorked(request.getCorrelationId(), request.getCreatedAt().toString(), resultPayload, registry, publishResult)
                .then();
    }

    public Mono<Void> markBatchAsWorked(String batchId, GatewayDownstreamService registry) {
        return nationalRegistriesBatchesRepository
                .updateToFinalStatus(batchId, SK, BatchesStatusEnum.WORKED)
                .doOnSuccess(batch -> log.debug("{} - batchId {} - updated status to WORKED", registry, batchId))
                .doOnError(error -> log.warn("{} - batchId {} - failed to update status to WORKED", registry, batchId, error))
                .then();
    }

    private Mono<Void> moveRequestToError(NationalRegistriesRequest request) {
        return nationalRegistriesRequestsRepository
                .updateStatusToError(request.getCorrelationId(), request.getCreatedAt().toString(), request.getRegistry())
                .then();
    }
}
