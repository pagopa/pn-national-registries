package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class IniPecBatchRequestService extends GatewayConverter {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final IniPecBatchSqsService iniPecBatchSqsService;

    private final int maxRetry;
    private final int maxBatchRequestSize;

    public IniPecBatchRequestService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository batchRequestRepository,
                                     IniPecBatchPollingRepository batchPollingRepository,
                                     InfoCamereClient infoCamereClient,
                                     IniPecBatchSqsService iniPecBatchSqsService,
                                     @Value("${pn.national-registries.inipec.max.batch.request.size}") int maxBatchRequestsize,
                                     @Value("${pn.national-registries.inipec.batch.request.max-retry}") int maxRetry) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.iniPecBatchSqsService = iniPecBatchSqsService;
        this.maxRetry = maxRetry;
        this.maxBatchRequestSize = maxBatchRequestsize;
    }

    @Scheduled(fixedDelayString = "${pn.national.registries.inipec.batch.request.delay}")
    @SchedulerLock(name="batchPecRequest", lockAtMostFor = "${pn.national-registries.inipec.batch.batch-pec-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch.batch-pec-request.lock-at-least}")
    public void batchPecRequest() {
        log.trace("IniPEC - batchPecRequest start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        page = getBatchRequest(lastEvaluatedKey);
        if (!page.items().isEmpty()) {
            String batchId = UUID.randomUUID().toString();
            execBatchRequest(page.items(), batchId)
                    .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId))
                    .block();
        } else {
            log.info("IniPEC - no batch request available");
        }
        log.trace("IniPEC - batchPecRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.request.recovery.delay}")
    @SchedulerLock(name = "recoveryBatchRequest", lockAtMostFor = "${pn.national-registries.inipec.batch.recovery-batch-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch.recovery-batch-request.lock-at-least}")
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
        return batchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, maxBatchRequestSize)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return new DigitalAddressException("IniPEC - can not get batch request");
                });
    }

    private Mono<Void> execBatchRequest(List<BatchRequest> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .doOnNext(item -> {
                    MDC.put("AWS_messageId", item.getAwsMessageId());
                    item.setStatus(BatchStatus.TAKEN_CHARGE.getValue());
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
                .flatMap(polling -> {
                    log.debug("IniPEC - batchId {} - created BatchPolling with pollingId: {}", batchId, pollingId);
                    return setBatchRequestStatusToWorking(batchId);
                })
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to create BatchPolling with pollingId: {}", batchId, pollingId, e));
    }

    private Mono<Void> setBatchRequestStatusToWorking(String batchId) {
            return batchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, BatchStatus.TAKEN_CHARGE)
                    .doOnNext(requests -> log.debug("IniPEC - batchId {} - updating {} requests in status {}", batchId, requests.size(), BatchStatus.WORKING))
                    .flatMapIterable(requests -> requests)
                    .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                    .flatMap(batchRequestRepository::update)
                    .doOnNext(r -> log.debug("IniPEC - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                    .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
                    .collectList()
                    .then();
    }

    private Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= maxRetry || (throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST)) {
                        r.setStatus(BatchStatus.ERROR.getValue());
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
                    return iniPecBatchSqsService.sendListToDlqQueue(l);
                });
    }
}
