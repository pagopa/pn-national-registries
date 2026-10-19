package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class InadBatchRequestJob {

    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;
    private final InadClient inadClient;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    @Scheduled(fixedDelayString = "${pn.national-registries.inad.batch-request.delay}")
    @SchedulerLock(name = "inadBatchRequest",
            lockAtMostFor = "${pn.national-registries.inad.batch-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inad.batch-request.lock-at-least}")
    public void batchRequest() {
        Instant now = Instant.now();
        processNextBatch(now, null)
                .doOnSuccess(ignored -> log.trace("INAD batch request job completed"))
                .doOnError(error -> log.error("INAD batch request job failed", error))
                .block();
    }

    private Mono<Void> processNextBatch(Instant now, Map<String, AttributeValue> lastEvaluatedKey) {
        int maxBatchSize = nationalRegistriesConfig.getInad().getMaxBatchRequestSize();
        return nationalRegistriesRequestsRepository
                .getRequestsByRegistryStatus(GatewayDownstreamService.INAD, lastEvaluatedKey, maxBatchSize)
                .flatMap(page -> processPage(now, page, maxBatchSize));
    }

    private Mono<Void> processPage(Instant now, Page<NationalRegistriesRequest> page, int maxBatchSize) {
        List<NationalRegistriesRequest> requests = page.items();
        if (requests.isEmpty()) {
            return CollectionUtils.isEmpty(page.lastEvaluatedKey())
                    ? Mono.empty()
                    : processNextBatch(now, page.lastEvaluatedKey());
        }

        if (!isEligible(requests, maxBatchSize, now)) {
            return Mono.empty();
        }

        return submitBatch(requests, now)
                // Requests have moved to WORKING, so a fresh query avoids skipping the next oldest records.
                .then(Mono.defer(() -> processNextBatch(now, null)));
    }

    private boolean isEligible(List<NationalRegistriesRequest> requests, int maxBatchSize, Instant now) {
        if (requests.size() == maxBatchSize) {
            return true;
        }
        Instant oldestRequestDeadline = requests.getFirst().getCreatedAt()
                .plusSeconds(nationalRegistriesConfig.getInad().getOldestRequestMaxWaitingSeconds());
        return !oldestRequestDeadline.isAfter(now);
    }

    private Mono<Void> submitBatch(List<NationalRegistriesRequest> requests, Instant now) {
        String batchId = UUID.nameUUIDFromBytes(String.join(":",
                        requests.stream().map(NationalRegistriesRequest::getCorrelationId).toList())
                        .getBytes(StandardCharsets.UTF_8))
                .toString();
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        request.setCodiciFiscali(requests.stream().map(NationalRegistriesRequest::getTaxId).toList());
        // INAD persists this reference with the provider request, making retries traceable to this batch.
        request.setPraticalReference(batchId);

        return inadClient.callMassiveEService(request)
                .flatMap(response -> persistBatch(response, batchId, requests, now))
                .doOnError(error -> log.error("INAD batch {} failed", batchId, error));
    }

    private Mono<Void> persistBatch(ResponseEntity<ResponseRequestListDigitalAddress> response,
                                    String batchId,
                                    List<NationalRegistriesRequest> requests,
                                    Instant now) {
        ResponseRequestListDigitalAddress providerResponse = response.getBody();
        if (providerResponse == null || providerResponse.getId() == null) {
            return Mono.error(new IllegalStateException("INAD returned no polling identifier for batch " + batchId));
        }

        NationalRegistriesBatch batch = new NationalRegistriesBatch();
        batch.setBatchId(batchId);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batch.setRegistry(GatewayDownstreamService.INAD.name());
        batch.setPollingId(providerResponse.getId());
        batch.setProviderLocation(response.getHeaders().getLocation() == null
                ? null
                : response.getHeaders().getLocation().toString());
        batch.setPracticalReferenceValue(batchId);
        batch.setBatchSize(requests.size());
        batch.setAttemptAfter(calculateFirstPollingAttempt(requests.size(), now));
        batch.setStatus(BatchesStatusEnum.REQUEST_SENT);
        batch.setRetryCount(0);
        batch.setInProgressRetryCount(0);
        batch.setTtl(now.plusSeconds(nationalRegistriesConfig.getTtl()).getEpochSecond());

        return nationalRegistriesBatchesRepository.putBatch(batch)
                .thenMany(reactor.core.publisher.Flux.fromIterable(requests)
                        // The repository conditionally accepts only NOT_WORKED -> WORKING transitions.
                        .concatMap(request -> nationalRegistriesRequestsRepository.updateStatusToWorking(
                                request.getCorrelationId(),
                                request.getCreatedAt().toString(),
                                batchId,
                                GatewayDownstreamService.INAD)))
                .then();
    }

    private Instant calculateFirstPollingAttempt(int batchSize, Instant now) {
        double delayPerTaxId = nationalRegistriesConfig.getInad().getPollingFirstAttemptDelaySecondsPerCf();
        int fixedDelay = nationalRegistriesConfig.getInad().getPollingFirstAttemptFixedDelaySeconds();
        return now.plusSeconds(Math.round((delayPerTaxId * batchSize) + fixedDelay))
                .truncatedTo(ChronoUnit.SECONDS);
    }
}
