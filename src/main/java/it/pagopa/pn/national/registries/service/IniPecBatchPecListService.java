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
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IniPecBatchPecListService {

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

    @Scheduled
    public void batchPecListRequest(){
        AtomicReference<Map<String, AttributeValue>> atomicLastKey = new AtomicReference<>(new HashMap<>());
        AtomicReference<Boolean> last = new AtomicReference<>(false);
        BooleanSupplier booleanSupplier = () -> !last.get();

        iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(atomicLastKey.get())
                .flatMap(batchRequestPage -> {
                    if(batchRequestPage.lastEvaluatedKey() != null){
                        atomicLastKey.set(batchRequestPage.lastEvaluatedKey());
                    }
                    else {
                        last.set(true);
                    }
                    String batchId = UUID.randomUUID().toString();
                    return setNewBatchId(batchRequestPage.items(),batchId);
                })
                .repeat(booleanSupplier)
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .subscribe();
    }


    private Mono<BatchPolling> setNewBatchId(List<BatchRequest> items, String batchId) {
        return iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(items, batchId)
                .filter(batchRequests -> !batchRequests.isEmpty())
                .flatMap(batchRequestWithNewBatchId -> {
                    RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
                    requestCfIniPec.setElencoCf(batchRequestWithNewBatchId.stream()
                            .filter(batchRequest -> batchRequest.getRetry()<=3)
                            .map(BatchRequest::getCf)
                            .collect(Collectors.toList()));
                    log.info("Calling ini pec with cf size: {} and batchId: {}",requestCfIniPec.getElencoCf().size(),batchId);
                    requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());
                    return callEservice(requestCfIniPec, batchId);

                })
                .doOnNext(batchPolling -> log.info("Created {} BatchRequests with batchId: {}",items.size(),batchId))
                .doOnError(batchPollingSignal -> log.error("Failed to create {} BatchRequests with batchId: {}",items.size(),batchId))
                .onErrorContinue((throwable, o) -> {});
    }

    private Mono<BatchPolling> callEservice(RequestCfIniPec requestCfIniPec, String batchId) {
        return iniPecClient.callEServiceRequestId(requestCfIniPec)
                .flatMap(responsePollingIdIniPec -> {
                    String pollingId = responsePollingIdIniPec.getIdentificativoRichiesta();
                    log.info("Called ini pec with batchId: {} and response pollingId: {}",batchId,pollingId);
                    return iniPecBatchPollingRepository.createBatchPolling(iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                            .doOnNext(batchPolling -> log.info("Created BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId))
                            .doOnError(batchPollingSignal -> log.error("Failed to create BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId));
                });
    }

    @Scheduled
    public void recoveryPrimoFlusso(){
        iniPecBatchRequestRepository.resetBatchIdForRecovery()
                .doOnNext(batchRequests -> batchPecListRequest())
                .subscribe();
    }
}
