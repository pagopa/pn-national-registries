package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
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

@Service
@Slf4j
public class IniPecBatchPecListService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;

    private final int maxRetry;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;

    public IniPecBatchPecListService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository batchRequestRepository,
                                     IniPecBatchPollingRepository batchPollingRepository,
                                     InfoCamereClient infoCamereClient,
                                     @Value("${pn.national-registries.inipec.batch.request.max-retry}") int maxRetry) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${pn.national.registries.inipec.batch.request.delay}")
    public void batchPecListRequest() {
        log.trace("IniPEC - batchPecListRequest start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            if (!page.items().isEmpty()) {
                String batchId = UUID.randomUUID().toString();
                setNewBatchId(page.items(), batchId).block();
            } else {
                log.info("IniPEC - no batch request available");
            }
        } while (!CollectionUtils.isEmpty(page.lastEvaluatedKey()));
        log.trace("IniPEC - batchPecListRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.request.recovery.delay}")
    public void recoveryPrimoFlusso() {
        batchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                })
                .flatMap(batchRequestRepository::update)
                .then()
                .doOnError(e -> log.error("IniPEC - can not recover batch", e))
                .doOnNext(t -> batchPecListRequest())
                .subscribe();
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return new IniPecException("IniPEC - can not get batch request");
                });
    }

    private Mono<Void> setNewBatchId(List<BatchRequest> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .map(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setBatchId(batchId);
                    item.setLastReserved(now);
                    return item;
                })
                .flatMap(item -> batchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip batch element: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .flatMap(requests -> {
                    RequestCfIniPec requestCfIniPec = createIniPecRequest(requests);
                    log.info("IniPEC - batchId {} - calling with {} cf", batchId, requestCfIniPec.getElencoCf().size());
                    return callEService(requestCfIniPec, batchId)
                            .onErrorResume(e -> incrementRetry(requests, batchId)
                                    .then(Mono.error(e)))
                            .flatMap(response -> createPolling(response, batchId))
                            .thenReturn(requests);
                })
                .doOnNext(requests -> log.info("IniPEC - batchId {} - called EService and batch size is: {}", batchId, requests.size()))
                .onErrorResume(e -> {
                    log.error("IniPEC - batchId {} - failed to execute batch", batchId);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<ResponsePollingIdIniPec> callEService(RequestCfIniPec requestCfIniPec, String batchId) {
        return infoCamereClient.callEServiceRequestId(requestCfIniPec)
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to call EService", batchId, e));
    }

    private RequestCfIniPec createIniPecRequest(List<BatchRequest> requests) {
        RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
        requestCfIniPec.setElencoCf(requests.stream()
                .map(BatchRequest::getCf)
                .toList());
        requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());
        return requestCfIniPec;
    }

    private Mono<Void> createPolling(ResponsePollingIdIniPec response, String batchId) {
        String pollingId = response.getIdentificativoRichiesta();
        log.info("IniPEC - batchId {} - called EService and response pollingId is {}", batchId, pollingId);
        return batchPollingRepository.createBatchPolling(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                .doOnNext(batchPolling -> log.debug("IniPEC - batchId {} - created BatchPolling with pollingId: {}", batchId, pollingId))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to create BatchPolling with pollingId: {}", batchId, pollingId, e))
                .then();
    }

    private Mono<Void> incrementRetry(List<BatchRequest> requests, String batchId) {
        return Flux.fromStream(requests.stream())
                .map(r -> {
                    int nextRetry = r.getRetry() != null ? r.getRetry() + 1 : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= maxRetry) {
                        r.setStatus(BatchStatus.ERROR.getValue());
                        log.debug("IniPEC - batchId {} - request {} status in {} (retry: {})", batchId, r.getCorrelationId(), r.getStatus(), r.getRetry());
                    }
                    return r;
                })
                .flatMap(batchRequestRepository::update)
                .then()
                .doOnNext(t -> log.debug("IniPEC - batchId {} - retry incremented", batchId))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to increment retry", batchId, e));
    }
}
