package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
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
import java.time.ZoneId;
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
    private static final String RECIPIENT_TYPE = "PG";
    private static final String DOMICILE_TYPE = "DIGITAL";

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
                .doOnSubscribe(s -> log.info("PG - DigitalAddress - sending {} requests to SQS", batchRequest.size()));
    }

    private Mono<Void> execBatchSendToSqs(List<BatchRequest> batchRequest, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(batchRequest.stream())
                .doOnNext(item -> {
                    item.setLastReserved(now);
                    item.setReservationId(reservationId);
                })
                .flatMap(item -> batchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("PG - DigitalAddress - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(item -> {
                    if (!BatchStatus.ERROR.getValue().equalsIgnoreCase(item.getStatus())) {
                        CodeSqsDto codeSqsDto = sqsService.toObject(item.getMessage(), CodeSqsDto.class);
                        return sqsService.pushToOutputQueue(codeSqsDto, item.getClientId())
                                .thenReturn(item)
                                .doOnNext(r -> {
                                    log.info("PG - DigitalAddress - pushed message for correlationId: {} and taxId: {}", item.getCorrelationId(), MaskDataUtils.maskString(item.getCf()));
                                    item.setSendStatus(BatchSendStatus.SENT.getValue());
                                })
                                .doOnError(e -> log.warn("PG - DigitalAddress - failed to push message for correlationId: {} and taxId: {}", item.getCorrelationId(), MaskDataUtils.maskString(item.getCf()), e))
                                .onErrorResume(e -> Mono.empty());
                    } else {
                        return sqsService.pushToInputDlqQueue(InternalCodeSqsDto.builder()
                                        .taxId(item.getCf())
                                        .correlationId(item.getCorrelationId())
                                        .referenceRequestDate(java.util.Date.from(item.getReferenceRequestDate().atZone(ZoneId.systemDefault()).toInstant()))
                                        .pnNationalRegistriesCxId(item.getClientId())
                                        .domicileType(DOMICILE_TYPE)
                                        .recipientType(RECIPIENT_TYPE)
                                        .build(), item.getClientId())
                                .thenReturn(item)
                                .doOnNext(r -> {
                                    log.info("PG - DigitalAddress - redrive to input queue message for correlationId: {} and taxId: {}", item.getCorrelationId(), MaskDataUtils.maskString(item.getCf()));
                                    item.setSendStatus(BatchSendStatus.SENT_TO_DLQ.getValue());
                                });
                    }
                })
                .flatMap(batchRequestRepository::update)
                .then();
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchRequestRepository.getBatchRequestToSend(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return new DigitalAddressException("IniPEC - can not get batch request");
                });
    }

    public Mono<Void> sendListToDlqQueue(List<BatchRequest> batchRequests) {
        return Flux.fromIterable(batchRequests)
                .map(this::sendToDlqQueue)
                .then();
    }

    public Mono<Void> sendToDlqQueue(BatchRequest batchRequest) {
        InternalCodeSqsDto internalCodeSqsDto = toInternalCodeSqsDto(batchRequest);
        if(batchRequest.getReferenceRequestDate() != null){
            internalCodeSqsDto.setReferenceRequestDate(java.util.Date.from(batchRequest.getReferenceRequestDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return sqsService.pushToInputDlqQueue(internalCodeSqsDto, batchRequest.getClientId())
                .then();
    }

    private InternalCodeSqsDto toInternalCodeSqsDto(BatchRequest batchRequest) {
        return InternalCodeSqsDto.builder()
                .taxId(batchRequest.getCf())
                .recipientType(RECIPIENT_TYPE)
                .domicileType(DOMICILE_TYPE)
                .pnNationalRegistriesCxId(batchRequest.getClientId())
                .correlationId(batchRequest.getCorrelationId())
                .build();
    }
}
