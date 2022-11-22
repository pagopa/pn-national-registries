package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

    public IniPecService(IniPecConverter iniPecConverter,
                         IniPecBatchRequestRepository iniPecBatchRequestRepository,
                         IniPecBatchPollingRepository iniPecBatchPollingRepository) {
        this.iniPecConverter = iniPecConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecBatchPollingRepository = iniPecBatchPollingRepository;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getDigitalAddress(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return iniPecBatchRequestRepository.createBatchRequestByCf(getDigitalAddressIniPECRequestBodyDto)
               .map(iniPecConverter::convertToGetAddressIniPecOKDto)
                .doOnNext(getDigitalAddressIniPECOKDto -> log.info("Created Batch Request with correlation id: {}",getDigitalAddressIniPECOKDto.getCorrelationId()));
    }

    public Mono<List<Void>> primoFlusso(){
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
                    List<BatchRequest> batchRequestsWithoutBatchId = batchRequestPage.items();
                    String batchId = UUID.randomUUID().toString();
                    return iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(batchRequestsWithoutBatchId, batchId)
                            .flatMap(batchRequestWithNewBatchId -> {
                        RequestCfIniPec requestCfIniPec = new RequestCfIniPec();
                        requestCfIniPec.setElencoCf(batchRequestWithNewBatchId.stream()
                                .filter(batchRequest -> batchRequest.getRetry()<=3)
                                .map(BatchRequest::getCf)
                                .collect(Collectors.toList()));
                        log.info("Calling ini pec with with {} cf and batchId: {}",requestCfIniPec.getElencoCf().size(),batchId);
                        requestCfIniPec.setDataOraRichiesta(LocalDateTime.now().toString());
                        //chiamata del client e mi ritorna polling id
                        //flat map convert dto
                        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
                        String pollingId = responsePollingIdIniPec.getIdentificativoRichiesta();
                        log.info("Called ini pec with batchId: {} and response pollingId: {}",batchId,pollingId);
                        //log.error("Failed to call ini pec with batchId: {}",batchId);
                        return iniPecBatchPollingRepository.createBatchPolling(iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId))
                        .doOnNext(batchPolling -> log.info("Created BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId))
                        .doOnEach(batchPollingSignal -> log.error("Failed to create BatchPolling with batchId: {} and pollingId: {}",batchId,pollingId));
                     })
                            .doOnNext(batchPolling -> log.info("Created {} BatchRequests with batchId: {}",batchRequestsWithoutBatchId.size(),batchId))
                            .doOnError(batchPollingSignal -> log.error("Failed to create {} BatchRequests with batchId: {}",batchRequestsWithoutBatchId.size(),batchId))
                            .then();
                })
                .repeat(booleanSupplier)
                .collectList();
    }


    public Mono<List<CodeSqsDto>> secondoFlusso() {
        return iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork().flatMap(batchPollingWithoutReservationId -> {

            String reservationId = UUID.randomUUID().toString();

            return iniPecBatchPollingRepository.setReservationIdToAndStatusToWorkBatchPolling(batchPollingWithoutReservationId, reservationId).flatMap(batchPollingWithReservationId -> {

                return iniPecBatchPollingRepository.getBatchPollingByReservationIdAndStatusToWork(reservationId).flatMap(batchsPollingWithReservationIdAndNotWorked -> {

                    return Flux.fromIterable(batchsPollingWithReservationIdAndNotWorked)
                            .flatMap(batchPollingToCall -> {
                                String pollingId = batchPollingToCall.getPollingId();
                                //effettuo la chiamata per inipec e costruisco la risposta
                                String cf = "";
                                CodeSqsDto codeSqsDto = new CodeSqsDto();
                                batchPollingToCall.setStatus(BatchStatus.WORKED.getValue());
                                return iniPecBatchPollingRepository.updateBatchPolling(batchPollingToCall).flatMap(batchPollingWorked -> {
                                    String batchId = batchPollingWorked.getBatchId();
                                    return iniPecBatchRequestRepository.getBatchRequestByBatchId(batchId).flatMap(batchRequestsToWorked -> {
                                        return iniPecBatchRequestRepository.setStatusToBatchRequests(batchRequestsToWorked,BatchStatus.WORKED.getValue()).map(batchRequests -> {
                                            return codeSqsDto;
                                        });
                                    });
                                });
                            }).collectList();
                });
            });
        });
    }

    public Mono<Void> recoveryPrimoFlusso(){
        return iniPecBatchRequestRepository.getBatchRequestToRecovery().flatMap(batchRequestsToRecovery -> {
            return iniPecBatchRequestRepository.resetBatchIdToBatchRequests(batchRequestsToRecovery).flatMap(batchRequests -> {
                return primoFlusso();
            });
        }).then();
    }

}
