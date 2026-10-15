package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PollingJob {

    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Scheduled(fixedDelayString = "${pn.national-registries.polling-delay}")
    @SchedulerLock(name = "polling", lockAtMostFor = "${pn.national-registries.polling-lock-at-most}", lockAtLeastFor = "${pn.national-registries.polling-lock-at-least}")
    public void polling() {
        log.trace("Polling job start");
        Instant now = Instant.now();
        retrieveAndProcessBatches(now, null)
                .doOnSuccess(ignored -> log.trace("Polling job completed"))
                .doOnError(ex -> log.error("Polling job failed", ex))
                .block();
    }

    private Mono<Void> retrieveAndProcessBatches(Instant now, Map<String, AttributeValue> lastEvaluatedKey) {
        return nationalRegistriesBatchesRepository
                .getBatchesToProcess(BatchesStatusEnum.REQUEST_SENT, now, lastEvaluatedKey)
                .flatMap(page ->
                        Flux.fromIterable(page.items())
                                .flatMap(this::takeCharge)
                                .then(Mono.defer(() -> {
                                    Map<String, AttributeValue> nextKey = page.lastEvaluatedKey();
                                    if (CollectionUtils.isEmpty(nextKey)) {
                                        return Mono.empty();
                                    }
                                    return retrieveAndProcessBatches(now, nextKey);
                                }))
                );
    }

    private Mono<Void> takeCharge(NationalRegistriesBatch batch) {
        batch.setStatus(BatchesStatusEnum.WORKING);
        return nationalRegistriesBatchesRepository
                .updateItem(batch, BatchesStatusEnum.REQUEST_SENT)
                .onErrorResume(ConditionalCheckFailedException.class, ex -> {
                    log.debug("Batch {} already taken by another worker", batch.getBatchId());
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.error("Error while processing batch {}", batch.getBatchId(), ex);
                    return Mono.empty();
                })
                .then();
    }
}
