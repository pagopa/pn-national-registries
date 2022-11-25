package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inipec.IniPecClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.repository.SqsRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
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
public class IniPecService {

    private final IniPecConverter iniPecConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final IniPecBatchPollingRepository iniPecBatchPollingRepository;
    private final SqsRepositoryImpl sqsRepository;
    private final IniPecClient iniPecClient;

    public IniPecService(IniPecConverter iniPecConverter,
                         IniPecBatchRequestRepository iniPecBatchRequestRepository,
                         IniPecBatchPollingRepository iniPecBatchPollingRepository,
                         SqsRepositoryImpl sqsRepository,
                         IniPecClient iniPecClient) {
        this.iniPecConverter = iniPecConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecBatchPollingRepository = iniPecBatchPollingRepository;
        this.sqsRepository = sqsRepository;
        this.iniPecClient = iniPecClient;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getDigitalAddress(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return iniPecBatchRequestRepository.createBatchRequestByCf(getDigitalAddressIniPECRequestBodyDto)
                .map(iniPecConverter::convertToGetAddressIniPecOKDto)
                .doOnNext(getDigitalAddressIniPECOKDto -> log.info("Created Batch Request with correlation id: {}",getDigitalAddressIniPECOKDto.getCorrelationId()));
    }

    public Mono<List<BatchPolling>> primoFlusso(){
        AtomicReference<Map<String, AttributeValue>> atomicLastKey = new AtomicReference<>(new HashMap<>());
        AtomicReference<Boolean> last = new AtomicReference<>(false);
        BooleanSupplier booleanSupplier = () -> !last.get();

        return iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(atomicLastKey.get())
                .flatMap(batchRequestPage -> {
                    if(batchRequestPage.lastEvaluatedKey() == null){
                        last.set(true);
                    }
                    else{
                        atomicLastKey.set(batchRequestPage.lastEvaluatedKey());
                    }
                    String batchId = UUID.randomUUID().toString();
                    return iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(batchRequestPage.items(), batchId)
                            .filter(batchRequests -> batchRequests.size() != 0)
                            .flatMap(batchRequestWithNewBatchId -> {
                                RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
                                requestCfIniPec.setElencoCf(batchRequestWithNewBatchId.stream()
                                        .filter(batchRequest -> batchRequest.getRetry()<=3)
                                        .map(BatchRequest::getCf)
                                        .collect(Collectors.toList()));
                                log.info("Calling ini pec with cf size: {} and batchId: {}",requestCfIniPec.getElencoCf().size(),batchId);
                                requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());

                                return iniPecClient.callEServiceRequestId(requestCfIniPec)
                                        .flatMap(responsePollingIdIniPec -> {
                                            String pollingId = responsePollingIdIniPec.getIdentificativoRichiesta();
                                            log.info("Called ini pec with batchId: {} and response pollingId: {}",batchId,pollingId);
                                            return iniPecBatchPollingRepository.createBatchPolling(iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                                                    .doOnNext(batchPolling -> log.info("Created BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId))
                                                    .doOnError(batchPollingSignal -> log.error("Failed to create BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId));
                                        });
                            })
                            .doOnNext(batchPolling -> log.info("Created {} BatchRequests with batchId: {}",batchRequestPage.items().size(),batchId))
                            .doOnError(batchPollingSignal -> log.error("Failed to create {} BatchRequests with batchId: {}",batchRequestPage.items().size(),batchId))
                            .onErrorContinue((throwable, o) -> {});
                })
                .repeat(booleanSupplier)
                .collectList();
    }


    public Mono<List<List<CodeSqsDto>>> secondoFlusso() {
        AtomicReference<Map<String, AttributeValue>> atomicLastKey = new AtomicReference<>(new HashMap<>());
        AtomicReference<Boolean> last = new AtomicReference<>(false);
        BooleanSupplier booleanSupplier = () -> !last.get();

        return iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork(atomicLastKey.get()).flatMap(batchPollingWithoutReservationId -> {
                    if(batchPollingWithoutReservationId.lastEvaluatedKey() == null){
                        last.set(true);
                    }
                    else{
                        atomicLastKey.set(batchPollingWithoutReservationId.lastEvaluatedKey());
                    }
                    String reservationId = UUID.randomUUID().toString();

                    return iniPecBatchPollingRepository.setReservationIdToAndStatusWorkingBatchPolling(batchPollingWithoutReservationId.items(), reservationId)
                            .filter(batchPollings -> batchPollings.size()!=0)
                            .flatMap(batchPollingWithReservationId -> {
                                BatchPolling batchPollingToCall = batchPollingWithReservationId.get(0);
                                String pollingId = batchPollingToCall.getPollingId();

                                return iniPecClient.callEServiceRequestPec(pollingId).flatMap(responsePecIniPec -> {
                                    batchPollingToCall.setStatus(BatchStatus.WORKED.getValue());

                                    return iniPecBatchPollingRepository.updateBatchPolling(batchPollingToCall).flatMap(batchPollingWorked -> {
                                        String batchId = batchPollingWorked.getBatchId();

                                        return iniPecBatchRequestRepository.getBatchRequestsByBatchIdAndSetStatus(batchId,BatchStatus.WORKED.getValue())
                                                .filter(batchRequests -> batchRequests.size() != 0)
                                                .map(batchRequests -> {
                                                    responsePecIniPec.setIdentificativoRichiesta(batchPollingWorked.getPollingId());
                                                    return iniPecConverter.convertoResponsePecToCodeSqsDto(batchRequests,responsePecIniPec);
                                                });
                                    });
                                });
                            });
                }).repeat(booleanSupplier)
                .collectList()
                .map(codeSqsDtos -> {
                    sqsRepository.push(codeSqsDtos);
                    return codeSqsDtos;
                });
    }

    public Mono<List<BatchPolling>> recoveryPrimoFlusso(){
        return iniPecBatchRequestRepository.resetBatchIdForRecovery().flatMap(batchRequests -> primoFlusso());
    }

}
