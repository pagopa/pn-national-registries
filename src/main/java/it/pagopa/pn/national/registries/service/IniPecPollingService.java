package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

@Service
@Slf4j
public class IniPecPollingService {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final IniPecBatchPollingRepository iniPecBatchPollingRepository;
    private final SqsService sqsService;
    private final InfoCamereClient infoCamereClient;

    public IniPecPollingService(InfoCamereConverter infoCamereConverter,
                                IniPecBatchRequestRepository iniPecBatchRequestRepository,
                                IniPecBatchPollingRepository iniPecBatchPollingRepository,
                                SqsService sqsService,
                                InfoCamereClient infoCamereClient) {
        this.infoCamereConverter = infoCamereConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecBatchPollingRepository = iniPecBatchPollingRepository;
        this.sqsService = sqsService;
        this.infoCamereClient = infoCamereClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void getPecList() {
        log.trace("getPecList start");
        Map<String, AttributeValue> lastEvaluatedKeyMap = new HashMap<>();
        boolean hasNext = true;

        while (hasNext) {
            hasNext = false;
            Page<BatchPolling> batchPollingPage = iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork(lastEvaluatedKeyMap)
                    .block();
            if(batchPollingPage != null && !batchPollingPage.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                setReservationId(batchPollingPage.items(), reservationId).block();
                if(batchPollingPage.lastEvaluatedKey()!=null){
                    hasNext = true;
                    lastEvaluatedKeyMap = batchPollingPage.lastEvaluatedKey();
                }
            }
        }
        log.trace("getPecList end");
    }


    private Mono<Void> setReservationId(List<BatchPolling> items, String reservationId) {
        return iniPecBatchPollingRepository.setReservationIdToAndStatusWorkingBatchPolling(items, reservationId)
                .flatMap(batchPollingWithReservationId -> {
                    BatchPolling batchPollingToCall = batchPollingWithReservationId.get(0);
                    String pollingId = batchPollingToCall.getPollingId();
                    log.info("Calling ini pec with pollingId: {}",pollingId);
                    return callEService(pollingId, batchPollingToCall);
                });
    }

    private Mono<Void> callEService(String pollingId, BatchPolling batchPollingToCall) {
        return infoCamereClient.callEServiceRequestPec(pollingId).flatMap(responsePecIniPec -> {
            log.info("Called ini pec with pollingId: {} and pecs size: {}",pollingId,responsePecIniPec.getElencoPec().size());
            batchPollingToCall.setStatus(BatchStatus.WORKED.getValue());
            return updateBatchPolling(batchPollingToCall, responsePecIniPec);
        });
    }

    private Mono<Void> updateBatchPolling(BatchPolling batchPollingToCall, ResponsePecIniPec responsePecIniPec) {
        return iniPecBatchPollingRepository.updateBatchPolling(batchPollingToCall).flatMap(batchPollingWorked -> {
            String batchId = batchPollingWorked.getBatchId();
            return iniPecBatchRequestRepository.getBatchRequestsToSend(batchId)
                    .flatMap(batchRequests -> {
                        responsePecIniPec.setIdentificativoRichiesta(batchPollingWorked.getPollingId());
                        return Flux.fromStream(batchRequests.stream())
                                .flatMap(request -> {
                                    CodeSqsDto codeSqsDto = infoCamereConverter.convertoResponsePecToCodeSqsDto(request, responsePecIniPec);
                                    return sqsService.push(codeSqsDto,request.getClientId())
                                            .flatMap(sendMessageResult -> iniPecBatchRequestRepository.setBatchRequestsStatus(request, BatchStatus.WORKED.getValue()))
                                            .doOnNext(batchRequest -> log.info("Pushed Message with correlationId: {} and taxId: {}",codeSqsDto.getCorrelationId(), MaskDataUtils.maskString(codeSqsDto.getTaxId())))
                                            .doOnError(throwable -> log.info("Failed to push Message with correlationId: {} and taxId: {} pushed",codeSqsDto.getCorrelationId(), MaskDataUtils.maskString(codeSqsDto.getTaxId())));
                                })
                                .then();
                    });
        });
    }
}
