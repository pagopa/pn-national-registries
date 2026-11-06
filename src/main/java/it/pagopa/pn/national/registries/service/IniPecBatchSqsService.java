package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;

@Slf4j
@Service
@RequiredArgsConstructor
public class IniPecBatchSqsService {

    private final IniPecBatchRequestRepository batchRequestRepository;
    private final SqsService sqsService;
    private final NationalRegistriesConfig config;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;
    private static final String DOMICILE_TYPE = "DIGITAL";


    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.sqs.recovery.delay}")
    @SchedulerLock(name = "recoveryBatchSendToSqs", lockAtMostFor = "${pn.national-registries.inipec.batch.sqs.recovery.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch.sqs.recovery.lock-at-least}")
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
                        .flatMap(request -> batchRequestRepository.update(request)
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
        Instant now = Instant.now();
        return Flux.fromStream(batchRequest.stream())
                .doOnNext(item -> {
                    item.setLastReserved(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
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
                                    log.info("PG - DigitalAddress - pushed message for correlationId: {}", item.getCorrelationId());
                                    item.setSendStatus(BatchSendStatus.SENT.getValue());
                                    item.setTtl(now.plusSeconds(config.getTtl()).getEpochSecond());
                                })
                                .doOnError(e -> log.warn("PG - DigitalAddress - failed to push message for correlationId: {}", item.getCorrelationId(), e))
                                .onErrorResume(e -> Mono.empty());
                    } else {
                        RecipientType recipientType = retrieveRecipientType(item.getCf(), item.getRecipientType());
                        return pushMessageToDLQ(item, recipientType)
                                .onErrorResume(e -> Mono.empty());
                    }
                })
                .flatMap(batchRequestRepository::update)
                .then();
    }

    private Mono<BatchRequest> pushMessageToDLQ(BatchRequest item, RecipientType recipientType) {
        Instant now = Instant.now();
        InternalCodeSqsDto internalCodeSqsDto = InternalCodeSqsDto.builder()
                .taxId(item.getCf())
                .correlationId(item.getCorrelationId().split(config.getInipec().getBatchRequestPkSeparator())[0])
                .pnNationalRegistriesCxId(item.getClientId())
                .domicileType(DOMICILE_TYPE)
                .build();

        if(Objects.nonNull(item.getReferenceRequestDate())){
            internalCodeSqsDto.setReferenceRequestDate(java.util.Date.from(item.getReferenceRequestDate().atZone(ZoneId.systemDefault()).toInstant()));
        }

        if(Objects.nonNull(recipientType)){
            internalCodeSqsDto.setRecipientType(recipientType.name());
        }

        return sqsService.pushToInputDlqQueue(internalCodeSqsDto, item.getClientId())
                .thenReturn(item)
                .doOnNext(batchRequest -> {
                    batchRequest.setSendStatus(BatchSendStatus.SENT_TO_DLQ.getValue());
                    batchRequest.setTtl(now.plusSeconds(config.getTtl()).getEpochSecond());
                });
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
                .flatMap(this::sendToDlqQueue)
                .then();
    }

    public Mono<BatchRequest> sendToDlqQueue(BatchRequest batchRequest) {
        RecipientType recipientType = retrieveRecipientType(batchRequest.getCf(), batchRequest.getRecipientType());
        return pushMessageToDLQ(batchRequest, recipientType)
                .flatMap(batchRequestRepository::update);
    }
}
