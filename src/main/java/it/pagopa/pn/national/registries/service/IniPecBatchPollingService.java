package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;

@Service
@Slf4j
public class IniPecBatchPollingService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final SqsService sqsService;

    private final int maxRetry;

    private static final int MAX_BATCH_POLLING_SIZE = 1;

    public IniPecBatchPollingService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository batchRequestRepository,
                                     IniPecBatchPollingRepository batchPollingRepository,
                                     InfoCamereClient infoCamereClient,
                                     SqsService sqsService,
                                     @Value("${pn.national-registries.inipec.batch.polling.max-retry}") int maxRetry) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.sqsService = sqsService;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.polling.delay}")
    public void batchPecPolling() {
        log.trace("IniPEC - batchPecPolling start");
        Page<BatchPolling> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchPolling(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                execBatchPolling(page.items(), reservationId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .block();
            } else {
                log.info("IniPEC - no batch polling available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("IniPEC - batchPecPolling end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.polling.recovery.delay}")
    public void recoveryBatchPolling() {
        batchPollingRepository.getBatchPollingToRecover()
                .flatMapIterable(polling -> polling)
                .doOnNext(polling -> {
                    polling.setStatus(BatchStatus.NOT_WORKED.getValue());
                    polling.setReservationId(null);
                })
                .flatMap(batchPollingRepository::update)
                .count()
                .doOnError(e -> log.error("IniPEC - can not recover polling", e))
                .doOnNext(v -> batchPecPolling())
                .subscribe(c -> log.info("IniPEC - executed batch recovery on {} polling", c),
                        e -> log.error("IniPEC - failed execution of batch polling recovery"));
    }

    private Page<BatchPolling> getBatchPolling(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(lastEvaluatedKey, MAX_BATCH_POLLING_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch polling - DynamoDB Mono<Page> is null");
                    return new IniPecException("IniPEC - can not get batch polling");
                });
    }

    private Mono<Void> execBatchPolling(List<BatchPolling> items, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .map(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setReservationId(reservationId);
                    item.setLastReserved(now);
                    return item;
                })
                .flatMap(item -> batchPollingRepository.setNewReservationIdToBatchPolling(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip pollingId: {}", item.getPollingId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(item -> callEService(item.getPollingId())
                        .onErrorResume(e -> incrementRetry(item)
                                .then(Mono.empty()))
                        .flatMap(response -> sendToQueueAndUpdateStatus(item, response))
                        .thenReturn(item))
                .doOnNext(item -> log.info("IniPEC - batchId {} - pollingId {} - called EService", item.getBatchId(), item.getPollingId()))
                .doOnError(e -> log.error("IniPEC - failed to execute polling", e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<IniPecPollingResponse> callEService(String pollingId) {
        return infoCamereClient.callEServiceRequestPec(pollingId)
                .doOnNext(response -> log.info("IniPEC - pollingId {} - response pec size: {}", pollingId, response.getElencoPec().size()))
                .doOnError(e -> log.warn("IniPEC - pollingId {} - failed to call EService", pollingId, e));
    }

    private Mono<Void> sendToQueueAndUpdateStatus(BatchPolling polling, IniPecPollingResponse response) {
        polling.setStatus(BatchStatus.WORKED.getValue());
        return batchRequestRepository.getBatchRequestByBatchIdAndStatusWorking(polling.getBatchId())
                .doOnNext(l -> log.info("IniPEC - batchId {} - pollingId {} - total batch available in status WORKING: {}", polling.getBatchId(), polling.getPollingId(), l.size()))
                .flatMapIterable(requests -> requests)
                .flatMap(request -> sendToSqs(request, polling, response))
                .count()
                .doOnNext(c -> log.info("IniPEC - batchId {} - pollingId {} - total batch pushed to SQS: {}", polling.getBatchId(), polling.getPollingId(), c))
                .then(Mono.just(polling))
                .flatMap(batchPollingRepository::update)
                .then();
    }

    private Mono<BatchRequest> sendToSqs(BatchRequest request, BatchPolling polling, IniPecPollingResponse response) {
        CodeSqsDto sqsDto = infoCamereConverter.convertoResponsePecToCodeSqsDto(request, response);
        return sqsService.push(sqsDto, request.getClientId())
                .doOnNext(sqsResponse -> log.info("IniPEC - pushed message to SQS with correlationId: {} and taxId: {}", sqsDto.getCorrelationId(), MaskDataUtils.maskString(sqsDto.getTaxId())))
                .doOnError(e -> log.error("IniPEC - failed to push message to SQS with correlationId: {} and taxId: {}", sqsDto.getCorrelationId(), MaskDataUtils.maskString(sqsDto.getTaxId()), e))
                .flatMap(sqsResponse -> {
                    request.setStatus(BatchStatus.WORKED.getValue());
                    return batchRequestRepository.update(request);
                })
                .onErrorResume(e -> {
                    polling.setRetry(0);
                    polling.setStatus(BatchStatus.WORKING.getValue());
                    return Mono.empty();
                });
    }

    private Mono<Void> incrementRetry(BatchPolling polling) {
        int nextRetry = polling.getRetry() != null ? polling.getRetry() + 1 : 1;
        polling.setRetry(nextRetry);
        if (nextRetry >= maxRetry) {
            polling.setStatus(BatchStatus.ERROR.getValue());
            log.debug("IniPEC - batchId {} - polling {} status in {} (retry: {})", polling.getBatchId(), polling.getPollingId(), polling.getStatus(), polling.getRetry());
        }
        return batchPollingRepository.update(polling)
                .doOnNext(p -> log.debug("IniPEC - batchId {} - pollingId {} - retry incremented", polling.getBatchId(), polling.getPollingId()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - pollingId {} - failed to increment retry", polling.getBatchId(), polling.getPollingId(), e))
                .filter(p -> BatchStatus.ERROR.getValue().equals(p.getStatus()))
                .flatMap(this::setBatchRequestInError);
    }

    private Mono<Void> setBatchRequestInError(BatchPolling polling) {
        return batchRequestRepository.getBatchRequestByBatchIdAndStatusWorking(polling.getBatchId())
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> request.setStatus(BatchStatus.ERROR.getValue()))
                .flatMap(batchRequestRepository::update)
                .doOnNext(r -> log.debug("IniPEC - batchId {} - set status in {}", r.getBatchId(), r.getStatus()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status ERROR", polling.getBatchId(), e))
                .flatMap(this::sendToSqs)
                .then();
    }

    private Mono<Void> sendToSqs(BatchRequest request) {
        CodeSqsDto sqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(request, ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS);
        return sqsService.push(sqsDto, request.getClientId())
                .doOnNext(sqsResponse -> log.info("IniPEC - pushed message to SQS with correlationId: {} and taxId: {}", sqsDto.getCorrelationId(), MaskDataUtils.maskString(sqsDto.getTaxId())))
                .doOnError(e -> log.error("IniPEC - failed to push message to SQS with correlationId: {} and taxId: {}", sqsDto.getCorrelationId(), MaskDataUtils.maskString(sqsDto.getTaxId()), e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
