package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
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
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
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
import static it.pagopa.pn.national.registries.utils.MetricUtils.logBatchRequestMetrics;

@Service
@CustomLog
@RequiredArgsConstructor
public class IniPecBatchRequestService extends GatewayConverter {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final IniPecBatchSqsService iniPecBatchSqsService;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    @Scheduled(fixedDelayString = "${pn.national.registries.inipec.batch.request.delay}")
    @SchedulerLock(name = "batchPecRequest", lockAtMostFor = "${pn.national-registries.inipec.batch.request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch.request.lock-at-least}")
    public void batchPecRequest() {
        log.trace("IniPEC - batchPecRequest start");
        collectBatchRequests()
                .flatMap(requests -> {
                    if (requests.isEmpty()) {
                        log.info("IniPEC - no batch request available");
                        return Mono.empty();
                    }
                    String batchId = UUID.randomUUID().toString();
                    return execBatchRequest(requests, batchId)
                            .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId));
                })
                .block();

        log.trace("IniPEC - batchPecRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.request.recovery.delay}")
    @SchedulerLock(name = "recoveryBatchRequest", lockAtMostFor = "${pn.national-registries.inipec.batch.request.recovery.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch.request.recovery.lock-at-least}")
    public void recoveryBatchRequest() {
        log.trace("IniPEC - recoveryBatchRequest start");
        batchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                })
                .flatMap(request -> batchRequestRepository.update(request)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .subscribe(c -> log.info("IniPEC - executed batch recovery on {} requests", c),
                        e -> log.error("IniPEC - failed execution of batch request recovery", e));
        log.trace("IniPEC - recoveryBatchRequest end");
    }

    private Mono<Page<BatchRequest>> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, nationalRegistriesConfig.getQueryLimit())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("IniPEC - can not get batch request - DynamoDB Mono<Page> is null");
                    return Mono.error(new DigitalAddressException("IniPEC - can not get batch request"));
                }));
    }

    private Mono<List<BatchRequest>> collectBatchRequests() {
        return getBatchRequest(new HashMap<>())
                .expand(page -> CollectionUtils.isEmpty(page.lastEvaluatedKey())
                        ? Mono.empty()
                        : getBatchRequest(page.lastEvaluatedKey()))
                .concatMapIterable(Page::items)
                .take(nationalRegistriesConfig.getInipec().getMaxBatchRequestSize())
                .collectList();
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
                            .doOnNext(res -> logBatchRequestMetrics(batchId, iniPecBatchRequest, false))
                            .doOnError(t -> logBatchRequestMetrics(batchId, iniPecBatchRequest, true))
                            .onErrorResume(t -> incrementAndCheckRetry(requests, t, batchId).then(Mono.error(t)))
                            .flatMap(response -> createPolling(response, batchId, requests, iniPecBatchRequest.getElencoCf().size()))
                            .thenReturn(requests);
                })
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

    private Mono<Void> createPolling(IniPecBatchResponse response, String batchId, List<BatchRequest> requests, Integer iniPecBatchRequestSize) {
        String pollingId = response.getIdentificativoRichiesta();
        log.info("IniPEC - batchId {} - creating BatchPolling with pollingId: {}", batchId, pollingId);
        return batchPollingRepository.create(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId, iniPecBatchRequestSize))
                .flatMap(polling -> {
                    log.debug("IniPEC - batchId {} - created BatchPolling with pollingId: {}", batchId, pollingId);
                    return setBatchRequestStatusToWorking(batchId, requests);
                })
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to create BatchPolling with pollingId: {}", batchId, pollingId, e));
    }

    private Mono<Void> setBatchRequestStatusToWorking(String batchId, List<BatchRequest> requests) {
        return Flux.fromIterable(requests)
                .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                .flatMap(batchRequestRepository::update, 1000)
                .doOnNext(r -> log.debug("IniPEC - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
                .collectList()
                .then();
    }

    public Mono<Void> handleRetryAndCheckDlq(List<BatchRequest> requests, Throwable throwable, String batchId) {
        return incrementAndCheckRetry(requests, throwable, batchId);
    }

    private Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= nationalRegistriesConfig.getInipec().getBatchRequestMaxRetry() || (throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST)) {
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
