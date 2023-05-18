package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;

@Slf4j
@Service
public class IniPecBatchPollingService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final IniPecBatchSqsService iniPecBatchSqsService;

    private final int maxRetry;

    private static final int MAX_BATCH_POLLING_SIZE = 1;

    public IniPecBatchPollingService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository batchRequestRepository,
                                     IniPecBatchPollingRepository batchPollingRepository,
                                     InfoCamereClient infoCamereClient,
                                     IniPecBatchSqsService iniPecBatchSqsService,
                                     @Value("${pn.national-registries.inipec.batch.polling.max-retry}") int maxRetry) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.iniPecBatchSqsService = iniPecBatchSqsService;
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
        log.trace("IniPEC - recoveryBatchPolling start");
        batchPollingRepository.getBatchPollingToRecover()
                .flatMapIterable(polling -> polling)
                .doOnNext(polling -> {
                    polling.setStatus(BatchStatus.NOT_WORKED.getValue());
                    polling.setReservationId(null);
                })
                .flatMap(polling -> batchPollingRepository.resetBatchPollingForRecovery(polling)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip recovery pollingId {} and batchId {}", polling.getPollingId(), polling.getBatchId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .doOnNext(c -> batchPecPolling())
                .subscribe(c -> log.info("IniPEC - executed batch recovery on {} polling", c),
                        e -> log.error("IniPEC - failed execution of batch polling recovery", e));
        log.trace("IniPEC - recoveryBatchPolling end");
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
                .flatMap(this::handlePolling)
                .doOnError(e -> log.error("IniPEC - reservationId {} - failed to execute polling on {} items", reservationId, items.size(), e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<Void> handlePolling(BatchPolling polling) {
        return callEService(polling.getBatchId(), polling.getPollingId())
                .onErrorResume(t -> incrementAndCheckRetry(polling, t).then(Mono.error(t)))
                .flatMap(response -> {
                    if(!infoCamereConverter.checkListPecInProgress(response)) {
                        return handleSuccessfulPolling(polling, response);
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<IniPecPollingResponse> callEService(String batchId, String pollingId) {
        return infoCamereClient.callEServiceRequestPec(pollingId)
                .doOnNext(response -> {
                     if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
                        if(infoCamereConverter.checkListPecInProgress(response)) {
                            log.info("IniPEC - batchId {} - pollingId {} - " + response.getDescription(), batchId, pollingId);
                        }
                        else {
                            throw new PnNationalRegistriesException(response.getDescription(), HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase() , null, null,
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                        }
                    }
                    else {
                        log.info("IniPEC - batchId {} - pollingId {} - response pec size: {}", batchId, pollingId, response.getElencoPec().size());
                    }
                })
                .doOnError(e -> log.warn("IniPEC - pollingId {} - failed to call EService", pollingId, e));
    }

    private Mono<Void> handleSuccessfulPolling(BatchPolling polling, IniPecPollingResponse response) {
        polling.setStatus(BatchStatus.WORKED.getValue());
        return batchPollingRepository.update(polling)
                .doOnNext(p -> log.debug("IniPEC - batchId {} - pollingId {} - updated status to WORKED", polling.getBatchId(), polling.getPollingId()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - pollingId {} - failed to update status to WORKED", polling.getBatchId(), polling.getPollingId(), e))
                .flatMap(p -> updateBatchRequest(p, BatchStatus.WORKED, getSqsOk(response)));
    }

    private Mono<Void> incrementAndCheckRetry(BatchPolling polling, Throwable throwable) {
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
                .flatMap(p -> {
                    String error;
                    if (throwable instanceof PnNationalRegistriesException exception) {
                        error = exception.getMessage();
                    } else {
                        error = ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;
                    }
                    return updateBatchRequest(p, BatchStatus.ERROR, getSqsKo(error));
                });
    }

    private Mono<Void> updateBatchRequest(BatchPolling polling, BatchStatus status, Function<BatchRequest, CodeSqsDto> sqsDtoProvider) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return batchRequestRepository.getBatchRequestByBatchIdAndStatus(polling.getBatchId(), BatchStatus.WORKING)
                .doOnNext(requests -> log.debug("IniPEC - batchId {} - updating {} requests in status {}", polling.getBatchId(), requests.size(), status))
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    CodeSqsDto sqsDto = sqsDtoProvider.apply(request);
                    request.setMessage(infoCamereConverter.convertCodeSqsDtoToString(sqsDto));
                    request.setStatus(status.getValue());
                    request.setSendStatus(BatchStatus.NOT_SENT.getValue());
                    request.setLastReserved(now);
                })
                .flatMap(batchRequestRepository::update)
                .doOnNext(r -> log.debug("IniPEC - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status {}", polling.getBatchId(), status, e))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(iniPecBatchSqsService::batchSendToSqs);
    }

    private Function<BatchRequest, CodeSqsDto> getSqsOk(IniPecPollingResponse response) {
        return request -> infoCamereConverter.convertResponsePecToCodeSqsDto(request, response);
    }

    private Function<BatchRequest, CodeSqsDto> getSqsKo(String error) {
        return request -> infoCamereConverter.convertIniPecRequestToSqsDto(request, error);
    }
}
