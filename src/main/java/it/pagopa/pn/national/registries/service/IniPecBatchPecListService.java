package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class IniPecBatchPecListService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final IniPecBatchPollingRepository iniPecBatchPollingRepository;
    private final InfoCamereClient infoCamereClient;

    public IniPecBatchPecListService(InfoCamereConverter infoCamereConverter,
                                     IniPecBatchRequestRepository iniPecBatchRequestRepository,
                                     IniPecBatchPollingRepository iniPecBatchPollingRepository,
                                     InfoCamereClient infoCamereClient) {
        this.infoCamereConverter = infoCamereConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecBatchPollingRepository = iniPecBatchPollingRepository;
        this.infoCamereClient = infoCamereClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void batchPecListRequest() {
        log.trace("batchPecListRequest start");
        Map<String, AttributeValue> lastEvaluatedKeyMap = new HashMap<>();
        boolean hasNext = true;

        while (hasNext) {
            hasNext = false;
            Page<BatchRequest> batchRequestPage = iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(lastEvaluatedKeyMap)
                    .block();
            if (batchRequestPage!= null && !batchRequestPage.items().isEmpty()) {
                if (batchRequestPage.lastEvaluatedKey() != null) {
                    hasNext = true;
                    lastEvaluatedKeyMap = batchRequestPage.lastEvaluatedKey();
                }
                String batchId = UUID.randomUUID().toString();
                setNewBatchId(batchRequestPage.items(), batchId).block();
            }
        }
        log.trace("batcPecListRequest end");
    }


    private Mono<Void> setNewBatchId(List<BatchRequest> items, String batchId) {
        return iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(items, batchId)
                .filter(batchRequests -> !batchRequests.isEmpty())
                .flatMap(batchRequestWithNewBatchId -> {
                    RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
                    requestCfIniPec.setElencoCf(batchRequestWithNewBatchId.stream()
                            .filter(batchRequest -> batchRequest.getRetry() <= 3)
                            .map(BatchRequest::getCf)
                            .toList());
                    requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());
                    log.info("Calling ini pec with cf size: {} and batchId: {}", requestCfIniPec.getElencoCf().size(), batchId);
                    return callEservice(requestCfIniPec, batchId);
                })
                .doOnNext(batchPolling -> log.info("Set batchId: {} to {} BatchRequests",batchId,items.size()))
                .doOnNext(batchPolling -> log.info("Failed to set batchId: {} to {} BatchRequests",batchId,items.size()))
                .onErrorContinue((throwable, o) -> {
                });
    }

    private Mono<Void> callEservice(RequestCfIniPec requestCfIniPec, String batchId) {
        return infoCamereClient.callEServiceRequestId(requestCfIniPec)
                .flatMap(responsePollingIdIniPec -> {
                    String pollingId = responsePollingIdIniPec.getIdentificativoRichiesta();
                    log.info("Called ini pec with batchId: {} and response pollingId: {}", batchId, pollingId);
                    return iniPecBatchPollingRepository.createBatchPolling(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                            .doOnNext(batchPolling -> log.info("Created BatchPolling with batchId: {} and pollingId: {}", batchId, pollingId))
                            .doOnError(batchPollingSignal -> log.error("Failed to create BatchPolling with batchId: {} and pollingId: {}", batchId, pollingId));
                }).then();
    }

    @Scheduled(fixedDelay = 30000)
    public void recoveryPrimoFlusso() {
        iniPecBatchRequestRepository.resetBatchIdForRecovery()
                .doOnNext(batchRequests -> batchPecListRequest())
                .subscribe();
    }
}
