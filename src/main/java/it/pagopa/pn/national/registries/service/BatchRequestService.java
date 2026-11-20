package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;

@Component
@Slf4j
@RequiredArgsConstructor
public class BatchRequestService {

    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    public Mono<Void> persistBatch(String batchId, String pollingId, List<NationalRegistriesRequest> requests, Instant now, GatewayDownstreamService registry, Instant attemptAfter){
        NationalRegistriesBatch batch = GatewayConverter.constructBatchEntity(batchId, requests, now, registry, pollingId);
        batch.setAttemptAfter(formatInstant(attemptAfter));

        return nationalRegistriesBatchesRepository.putBatch(batch)
                .thenMany(reactor.core.publisher.Flux.fromIterable(requests)
                        // The repository conditionally accepts only NOT_WORKED -> WORKING transitions.
                        .flatMap(request -> nationalRegistriesRequestsRepository.updateStatusToWorking(
                                request.getCorrelationId(),
                                request.getCreatedAt().toString(),
                                batchId,
                                registry), 100 ))
                .then();
    }

    public Mono<Void> handleBatchError(PnNationalRegistriesException error, List<NationalRegistriesRequest> requests, GatewayDownstreamService registry, int maxRetry, int retryAfter) {

        if (error.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return Flux.fromIterable(requests)
                    .concatMap(request -> {
                        log.info("Batch request with correlationId {} failed with BAD_REQUEST, updating status to ERROR", request.getCorrelationId());
                        return nationalRegistriesRequestsRepository.updateStatusToError(
                                request.getCorrelationId(),
                                request.getCreatedAt().toString(),
                                registry,
                                null);
                    })
                    .then();
        }

        return incrementAndCheckRetryForEachRequest(requests, maxRetry, retryAfter);
    }

    public Mono<Void> incrementAndCheckRetryForEachRequest(List<NationalRegistriesRequest> requests, Integer batchRequestMaxRetry, Integer requestRetryAfter) {
        return Flux.fromIterable(requests)
                .concatMap(request -> checkRetryAndUpdateItem(request, batchRequestMaxRetry, requestRetryAfter))
                .then();
    }

    public Mono<Void> checkRetryAndUpdateItem(NationalRegistriesRequest request, Integer maxRetry, Integer requestRetryAfter) {
        int retry = Optional.ofNullable(request.getRetryCount()).orElse(0) + 1;
        if (retry >= maxRetry) {
            log.warn("{} - correlationId {} - max retry reached ({}/{})", request.getRegistry(), request.getCorrelationId(), retry, maxRetry);
            return nationalRegistriesRequestsRepository.updateStatusToError(request.getCorrelationId(), request.getCreatedAt().toString(), request.getRegistry(), retry).then();
        }
        Instant attemptAfter = Instant.now().plusSeconds(requestRetryAfter);
        return nationalRegistriesRequestsRepository.resetStatusForRetry(request.getCorrelationId(), request.getCreatedAt().toString(), retry, attemptAfter, request.getRegistry())
                .doOnSuccess(r -> log.info("{}- correlationId {} - reset for retry ({}/{})", request.getRegistry(), request.getCorrelationId(), retry, maxRetry))
                .doOnError(e -> log.warn("{}- correlationId {} - failed to reset for retry",request.getRegistry(), request.getCorrelationId(), e))
                .then();
    }
}
