package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;

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
        retrieveAndProcessBatches(now)
                .doOnSuccess(ignored -> log.trace("Polling job completed"))
                .doOnError(ex -> log.error("Polling job failed", ex))
                .block();
    }

    private Mono<Void> retrieveAndProcessBatches(Instant now) {
        return Flux.from(nationalRegistriesBatchesRepository.getBatchesToProcess(BatchesStatusEnum.REQUEST_SENT, now))
                .flatMapIterable(Page::items)
                .flatMap(this::takeCharge)
                .then();
    }

    private Mono<Void> takeCharge(NationalRegistriesBatch batch) {
        return nationalRegistriesBatchesRepository
                .updateItem(batch, BatchesStatusEnum.WORKING)
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
