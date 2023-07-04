package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Slf4j
@Service
public class IniPecBatchSqsService {

    private final IniPecBatchRequestRepository batchRequestRepository;
    private final SqsService sqsService;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;

    public IniPecBatchSqsService(IniPecBatchRequestRepository batchRequestRepository,
                                 SqsService sqsService) {
        this.batchRequestRepository = batchRequestRepository;
        this.sqsService = sqsService;
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.sqs.recovery.delay}")
    public void recoveryBatchSendToSqs() {
        log.trace("IniPEC - recoveryBatchSendToSqs start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                Flux.fromStream(page.items().stream())
                        .doOnNext(request -> request.setReservationId(null))
                        .flatMap(request -> batchRequestRepository.resetBatchRequestForRecovery(request)
                                .doOnError(ConditionalCheckFailedException.class,
                                        e -> log.info("IniPEC - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                                .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                        .collectList()
                        .flatMap(requestToRecover -> execBatchSendToSqs(requestToRecover, reservationId)
                                .thenReturn(requestToRecover.size()))
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .subscribe(c -> log.info("IniPEC - executed batch SQS recovery on {} requests", c),
                                e -> log.error("IniPEC - failed execution of batch request SQS recovery", e));
            } else {
                log.info("IniPEC - no batch request to send to SQS to recover");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("IniPEC - recoveryBatchSendToSqs end");
    }

    public Mono<Void> batchSendToSqs(List<BatchRequest> batchRequest) {
        String reservationId = UUID.randomUUID().toString();
        return execBatchSendToSqs(batchRequest, reservationId)
                .doOnSubscribe(s -> log.info("IniPEC - sending {} requests to SQS", batchRequest.size()));
    }

    private Mono<Void> execBatchSendToSqs(List<BatchRequest> batchRequest, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(batchRequest.stream())
                .filter(item -> !BatchStatus.PEC_NOT_FOUND.getValue().equalsIgnoreCase(item.getSendStatus()))
                .doOnNext(item -> {
                    item.setLastReserved(now);
                    item.setReservationId(reservationId);
                })
                .flatMap(item -> batchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(item -> sqsService.push(item.getMessage(), item.getClientId())
                        .thenReturn(item)
                        .doOnNext(r -> log.info("IniPEC - pushed message for correlationId: {} and taxId: {}", item.getCorrelationId(), MaskDataUtils.maskString(item.getCf())))
                        .doOnError(e -> log.warn("IniPEC - failed to push message for correlationId: {} and taxId: {}", item.getCorrelationId(), MaskDataUtils.maskString(item.getCf()), e))
                        .onErrorResume(e -> Mono.empty()))
                .doOnNext(item -> item.setSendStatus(BatchStatus.SENT.getValue()))
                .flatMap(batchRequestRepository::update)
                .then();
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchRequestRepository.getBatchRequestToSend(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return new IniPecException("IniPEC - can not get batch request");
                });
    }
}
