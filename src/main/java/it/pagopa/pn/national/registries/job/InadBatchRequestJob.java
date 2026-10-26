package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;

@Component
@Slf4j
@RequiredArgsConstructor
public class InadBatchRequestJob {

    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;
    private final InadClient inadClient;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    @Scheduled(fixedDelayString = "${pn.national-registries.inad.batch-request.delay}")
    @SchedulerLock(name = "inadRequests",
            lockAtMostFor = "${pn.national-registries.inad.batch-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inad.batch-request.lock-at-least}")
    public void batchRequest() {
        MDCUtils.clearMDCKeys();
        String executionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, executionId);
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, "INAD_BATCH_REQUEST");
        Mono<Void> executeBatch = processNextBatch(now)
                .doOnSuccess(ignored -> log.trace("INAD batch request job completed"))
                .doOnError(error -> log.error("INAD batch request job failed", error));
        MDCUtils.addMDCToContextAndExecute(executeBatch).block();
    }

    private Mono<Void> processNextBatch(Instant now) {
        int maxBatchSize = nationalRegistriesConfig.getInad().getMaxBatchRequestSize();
        int queryLimit = nationalRegistriesConfig.getQueryLimit();

        return Flux.from(nationalRegistriesRequestsRepository
                        .getRequestsByRegistryStatus(GatewayDownstreamService.INAD, queryLimit))
                .concatMapIterable(Page::items)
                .buffer(maxBatchSize)
                .concatMap(requests -> isEligible(requests, maxBatchSize, now)
                        ? submitBatch(requests, now)
                        : Mono.empty())
                .then();
    }

    private boolean isEligible(List<NationalRegistriesRequest> requests, int batchSize, Instant now) {
        if (CollectionUtils.isEmpty(requests)) {
            return false;
        }
        if (requests.size() == batchSize) {
            log.info("Inad batch request is eligible for batchSize, requestsSize: {}, batchSize: {}", requests.size(), batchSize);
            return true;
        }
        Instant createdAtOldestRequest = requests.getFirst().getCreatedAt();
        Instant oldestRequestDeadline = createdAtOldestRequest
                .plusSeconds(nationalRegistriesConfig.getInad().getOldestRequestMaxWaitingSeconds());
        boolean waitIsOver = !oldestRequestDeadline.isAfter(now);
        if (waitIsOver) {
            log.info("Inad batch request is eligible for waitIsOver, createdAtOldestRequest: {}, MaxWaitingSeconds: {}, " +
                    "oldestRequestDeadline: {}",
                    createdAtOldestRequest, nationalRegistriesConfig.getInad().getOldestRequestMaxWaitingSeconds(), oldestRequestDeadline);
        }
        return waitIsOver;
    }

    private Mono<Void> submitBatch(List<NationalRegistriesRequest> requests, Instant now) {
        String batchId = UUID.randomUUID().toString();
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        request.setCodiciFiscali(requests.stream().map(NationalRegistriesRequest::getTaxId).toList());
        // INAD persists this reference with the provider request, making retries traceable to this batch.
        request.setPraticalReference(batchId);

        return inadClient.callMassiveEService(request)
                .onErrorResume(PnNationalRegistriesException.class,throwable -> checkIfIsBadRequestAndMarkRequestAsError(throwable, requests))
                .flatMap(response -> persistBatch(response, batchId, requests, now))
                .doOnError(error -> log.error("INAD batch {} failed", batchId, error));
    }

    private Mono<ResponseRequestListDigitalAddress> checkIfIsBadRequestAndMarkRequestAsError(PnNationalRegistriesException throwable, List<NationalRegistriesRequest> requests) {
        boolean badRequest = throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST;
        if(badRequest){
            return Flux.fromIterable(requests)
                    .concatMap(request -> nationalRegistriesRequestsRepository.updateStatusToError(
                            request.getCorrelationId(),
                            request.getCreatedAt().toString(),
                            GatewayDownstreamService.INAD))
                    .then(Mono.empty());
        }
        return Mono.error(throwable);

    }

    private Mono<Void> persistBatch(ResponseRequestListDigitalAddress providerResponse,
                                    String batchId,
                                    List<NationalRegistriesRequest> requests,
                                    Instant now) {
        if (!Objects.nonNull(providerResponse) || !StringUtils.hasText(providerResponse.getId())) {
            return Mono.error(new PnInternalException("INAD returned no polling identifier for batch " + batchId, 400, ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID));
        }

        NationalRegistriesBatch batch = getBatch(batchId, requests, now, providerResponse);

        return nationalRegistriesBatchesRepository.putBatch(batch)
                .thenMany(reactor.core.publisher.Flux.fromIterable(requests)
                        // The repository conditionally accepts only NOT_WORKED -> WORKING transitions.
                        .flatMap(request -> nationalRegistriesRequestsRepository.updateStatusToWorking(
                                request.getCorrelationId(),
                                request.getCreatedAt().toString(),
                                batchId,
                                GatewayDownstreamService.INAD)))
                .then();
    }

    private NationalRegistriesBatch getBatch(String batchId, List<NationalRegistriesRequest> requests,
                                             Instant now, ResponseRequestListDigitalAddress providerResponse) {
        NationalRegistriesBatch batch = new NationalRegistriesBatch();
        batch.setBatchId(batchId);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batch.setRegistry(GatewayDownstreamService.INAD.name());
        batch.setPollingId(providerResponse.getId());
        batch.setPracticalReferenceValue(batchId);
        batch.setBatchSize(requests.size());
        batch.setAttemptAfter(formatInstant(calculateFirstPollingAttempt(requests.size(), now)));
        batch.setStatus(BatchesStatusEnum.REQUEST_SENT);
        batch.setErrorRetryCount(0);
        batch.setInProgressRetryCount(0);
        return batch;
    }

    private Instant calculateFirstPollingAttempt(int batchSize, Instant now) {
        double delayPerTaxId = nationalRegistriesConfig.getInad().getPollingFirstAttemptDelaySecondsPerCf();
        int fixedDelay = nationalRegistriesConfig.getInad().getPollingFirstAttemptFixedDelaySeconds();
        return now.plusSeconds(Math.round((delayPerTaxId * batchSize) + fixedDelay))
                .truncatedTo(ChronoUnit.SECONDS);
    }
}
