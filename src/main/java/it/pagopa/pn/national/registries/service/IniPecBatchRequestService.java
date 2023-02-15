package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchResponse;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
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
public class IniPecBatchRequestService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final IniPecBatchSqsService iniPecBatchSqsService;

    private final int maxRetry;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;

    public IniPecBatchRequestService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository batchRequestRepository,
                                     IniPecBatchPollingRepository batchPollingRepository,
                                     InfoCamereClient infoCamereClient,
                                     IniPecBatchSqsService iniPecBatchSqsService,
                                     @Value("${pn.national-registries.inipec.batch.request.max-retry}") int maxRetry) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.iniPecBatchSqsService = iniPecBatchSqsService;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${pn.national.registries.inipec.batch.request.delay}")
    public void batchPecRequest() {
        log.trace("IniPEC - batchPecRequest start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String batchId = UUID.randomUUID().toString();
                execBatchRequest(page.items(), batchId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId))
                        .block();
            } else {
                log.info("IniPEC - no batch request available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("IniPEC - batchPecRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.request.recovery.delay}")
    public void recoveryBatchRequest() {
        log.trace("IniPEC - recoveryBatchRequest start");
        batchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                })
                .flatMap(request -> batchRequestRepository.resetBatchRequestForRecovery(request)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .doOnNext(c -> batchPecRequest())
                .subscribe(c -> log.info("IniPEC - executed batch recovery on {} requests", c),
                        e -> log.error("IniPEC - failed execution of batch request recovery", e));
        log.trace("IniPEC - recoveryBatchRequest end");
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return new IniPecException("IniPEC - can not get batch request");
                });
    }

    private Mono<Void> execBatchRequest(List<BatchRequest> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .doOnNext(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setBatchId(batchId);
                    item.setLastReserved(now);
                })
                .flatMap(item -> batchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .flatMap(requests -> {
                    IniPecBatchRequest iniPecBatchRequest = createIniPecRequest(requests);
                    log.info("IniPEC - batchId {} - calling with {} cf", batchId, iniPecBatchRequest.getElencoCf().size());
                    return callEService(iniPecBatchRequest, batchId)
                            .onErrorResume(t -> incrementAndCheckRetry(requests, t, batchId).then(Mono.error(t)))
                            .flatMap(response -> createPolling(response, batchId))
                            .thenReturn(requests);
                })
                .doOnNext(requests -> log.info("IniPEC - batchId {} - called EService and batch size is: {}", batchId, requests.size()))
                .doOnError(e -> log.error("IniPEC - batchId {} - failed to execute batch", batchId, e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<IniPecBatchResponse> callEService(IniPecBatchRequest iniPecBatchRequest, String batchId) {
        return infoCamereClient.callEServiceRequestId(iniPecBatchRequest)
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to call EService", batchId, e));
    }

    private IniPecBatchRequest createIniPecRequest(List<BatchRequest> requests) {
        IniPecBatchRequest iniPecBatchRequest = new IniPecBatchRequest();
        iniPecBatchRequest.setElencoCf(requests.stream()
                .map(request -> {
                    IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
                    iniPecCf.setCf(request.getCf());
                    return iniPecCf;
                })
                .toList());
        iniPecBatchRequest.setDataOraRichiesta(LocalDateTime.now().toString());
        return iniPecBatchRequest;
    }

    private Mono<Void> createPolling(IniPecBatchResponse response, String batchId) {
        String pollingId = response.getIdentificativoRichiesta();
        log.info("IniPEC - batchId {} - creating BatchPolling with pollingId: {}", batchId, pollingId);
        return batchPollingRepository.create(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                .doOnNext(polling -> log.debug("IniPEC - batchId {} - created BatchPolling with pollingId: {}", batchId, pollingId))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to create BatchPolling with pollingId: {}", batchId, pollingId, e))
                .then();
    }

    private Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = r.getRetry() != null ? r.getRetry() + 1 : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= maxRetry) {
                        String error;
                        if (throwable instanceof PnNationalRegistriesException exception) {
                            error = exception.getMessage();
                        } else {
                            error = ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;
                        }
                        CodeSqsDto sqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(r, error);
                        r.setMessage(infoCamereConverter.convertCodeSqsDtoToString(sqsDto));
                        r.setStatus(BatchStatus.ERROR.getValue());
                        r.setSendStatus(BatchStatus.NOT_SENT.getValue());
                        r.setLastReserved(now);
                        log.debug("IniPEC - batchId {} - request {} status in {} (retry: {})", batchId, r.getCorrelationId(), r.getStatus(), r.getRetry());
                    }
                })
                .flatMap(batchRequestRepository::update)
                .doOnNext(r -> log.debug("IniPEC - batchId {} - retry incremented for correlationId: {}", batchId, r.getCorrelationId()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to increment retry", batchId, e))
                .filter(r -> BatchStatus.ERROR.getValue().equals(r.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(l -> {
                    log.debug("IniPEC - there is at least one request in ERROR - call batch to send to SQS");
                    return iniPecBatchSqsService.batchSendToSqs(l);
                });
    }
}