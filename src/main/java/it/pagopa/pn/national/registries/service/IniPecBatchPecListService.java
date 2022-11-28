package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inipec.IniPecClient;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class IniPecBatchPecListService {

    private static final Integer FIXED_DELAY = 300000;

    private final IniPecConverter iniPecConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final IniPecBatchPollingRepository iniPecBatchPollingRepository;
    private final IniPecClient iniPecClient;

    public IniPecBatchPecListService(IniPecConverter iniPecConverter,
                                     IniPecBatchRequestRepository iniPecBatchRequestRepository,
                                     IniPecBatchPollingRepository iniPecBatchPollingRepository,
                                     IniPecClient iniPecClient) {
        this.iniPecConverter = iniPecConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecBatchPollingRepository = iniPecBatchPollingRepository;
        this.iniPecClient = iniPecClient;
    }

    @Scheduled(fixedDelay = FIXED_DELAY)
    public void batchPecListRequest() {
        Map<String, AttributeValue> lastEvaluatedKeyMap = new HashMap<>();
        boolean hasNext = true;

        while (hasNext) {
            hasNext = false;
            Page<BatchRequest> batchRequestPage = iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(lastEvaluatedKeyMap)
                    .block();
            if (batchRequestPage != null) {
                if (batchRequestPage.lastEvaluatedKey() != null) {
                    hasNext = true;
                    lastEvaluatedKeyMap = batchRequestPage.lastEvaluatedKey();
                }
                String batchId = UUID.randomUUID().toString();
                setNewBatchId(batchRequestPage.items(), batchId).block();
            }
        }
    }


    private Mono<Void> setNewBatchId(List<BatchRequest> items, String batchId) {
        return iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(items, batchId)
                .filter(batchRequests -> !batchRequests.isEmpty())
                .flatMap(batchRequestWithNewBatchId -> {
                    RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
                    requestCfIniPec.setElencoCf(batchRequestWithNewBatchId.stream()
                            .filter(batchRequest -> batchRequest.getRetry() <= 3)
                            .map(BatchRequest::getCf)
                            .collect(Collectors.toList()));
                    log.info("Calling ini pec with cf size: {} and batchId: {}", requestCfIniPec.getElencoCf().size(), batchId);
                    requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());
                    return callEservice(requestCfIniPec, batchId);
                })
                .doOnNext(batchPolling -> log.info("Created {} BatchRequests with batchId: {}", items.size(), batchId))
                .doOnError(batchPollingSignal -> log.error("Failed to create {} BatchRequests with batchId: {}", items.size(), batchId))
                .onErrorContinue((throwable, o) -> {
                });
    }

    private Mono<Void> callEservice(RequestCfIniPec requestCfIniPec, String batchId) {
        return iniPecClient.callEServiceRequestId(requestCfIniPec)
                .flatMap(responsePollingIdIniPec -> {
                    String pollingId = responsePollingIdIniPec.getIdentificativoRichiesta();
                    log.info("Called ini pec with batchId: {} and response pollingId: {}", batchId, pollingId);
                    return iniPecBatchPollingRepository.createBatchPolling(iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                            .doOnNext(batchPolling -> log.info("Created BatchPolling with batchId: {} and pollingId: {}", batchId, pollingId))
                            .doOnError(batchPollingSignal -> log.error("Failed to create BatchPolling with batchId: {} and pollingId: {}", batchId, pollingId));
                }).then();
    }

    @Scheduled(fixedDelay = FIXED_DELAY)
    public void recoveryPrimoFlusso() {
        iniPecBatchRequestRepository.resetBatchIdForRecovery()
                .doOnNext(batchRequests -> batchPecListRequest())
                .subscribe();
    }
}
