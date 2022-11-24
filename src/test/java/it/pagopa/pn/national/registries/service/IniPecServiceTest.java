package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepositoryImpl;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepositoryImpl;
import it.pagopa.pn.national.registries.repository.SqsRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class IniPecServiceTest {

    @InjectMocks
    IniPecService iniPecService;

    @Mock
    IniPecService iniPecService2;
    @Mock
    IniPecConverter iniPecConverter;

    @Mock
    IniPecBatchRequestRepositoryImpl iniPecBatchRequestRepository;

    @Mock
    IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository;

    @Mock
    SqsRepositoryImpl sqsRepository;

    @Test
    void testGetDigitalAddress(){
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filterDto = new CheckTaxIdRequestBodyFilterDto();
        filterDto.setTaxId("taxId");
        requestBodyDto.setFilter(filterDto);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setBatchId("batchId");
        batchRequest.setStatus("status");
        batchRequest.setTimeStamp(LocalDateTime.now());
        batchRequest.setRetry(0);
        batchRequest.setTtl(LocalDateTime.now());
        batchRequest.setCorrelationId("correlationId");

        GetDigitalAddressIniPECOKDto getDigitalAddressIniPECOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressIniPECOKDto.setCorrelationId("correlationId");

        when(iniPecBatchRequestRepository.createBatchRequestByCf(requestBodyDto)).thenReturn(Mono.just(batchRequest));
        when(iniPecConverter.convertToGetAddressIniPecOKDto(batchRequest)).thenReturn(getDigitalAddressIniPECOKDto);

        StepVerifier.create(iniPecService.getDigitalAddress(requestBodyDto))
                .expectNext(getDigitalAddressIniPECOKDto).verifyComplete();
    }

    @Test
    void testPrimoFlusso(){
        AtomicReference<Map<String, AttributeValue>> atomicLastKey = new AtomicReference<>(new HashMap<>());
        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        batchRequest.setTtl(LocalDateTime.now());
        batchRequest.setRetry(0);
        batchRequest.setCf("taxId");
        batchRequest.setBatchId("batchId");
        batchRequest.setCorrelationId("correlationId");
        batchRequests.add(batchRequest);
        ArrayList<BatchPolling> batchPollings = new ArrayList<>();
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setReservationId("reservationId");
        batchPolling.setStatus("status");
        batchPolling.setBatchId("batchId");
        batchPollings.add(batchPolling);

        when(iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(atomicLastKey.get())).thenReturn(Mono.just(Page.create(batchRequests)));

        when(iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(anyList(),anyString())).thenReturn(Mono.just(batchRequests));

        when(iniPecConverter.createBatchPollingByBatchIdAndPollingId(anyString(),anyString())).thenReturn(batchPolling);

        when(iniPecBatchPollingRepository.createBatchPolling(any())).thenReturn(Mono.just(batchPolling));

        StepVerifier.create(iniPecService.primoFlusso()).expectNext(batchPollings).verifyComplete();
    }

    @Test
    void testSecondoFlusso(){
        AtomicReference<Map<String, AttributeValue>> atomicLastKey = new AtomicReference<>(new HashMap<>());
        ArrayList<BatchPolling> batchPollings = new ArrayList<>();
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setReservationId("reservationId");
        batchPolling.setStatus("status");
        batchPolling.setTimeStamp(LocalDateTime.now());
        batchPolling.setBatchId("batchId");
        batchPollings.add(batchPolling);
        Page<BatchPolling> page = Page.create(batchPollings);

        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        batchRequest.setTtl(LocalDateTime.now());
        batchRequest.setRetry(0);
        batchRequest.setCf("taxId");
        batchRequest.setBatchId("batchId");
        batchRequest.setCorrelationId("correlationId");
        batchRequests.add(batchRequest);

        when(iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork(atomicLastKey.get())).thenReturn(Mono.just(page));

        when(iniPecBatchPollingRepository.setReservationIdToAndStatusWorkingBatchPolling(anyList(),anyString())).thenReturn(Mono.just(batchPollings));

        when(iniPecBatchPollingRepository.updateBatchPolling(batchPolling)).thenReturn(Mono.just(batchPolling));

        when(iniPecBatchRequestRepository.getBatchRequestsByBatchIdAndSetStatus("batchId", BatchStatus.WORKED.getValue())).thenReturn(Mono.just(batchRequests));

        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        responsePecIniPec.setIdentificativoRichiesta(batchPolling.getPollingId());
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setStatus("ok");
        codeSqsDto.setCorrelationId("correlationId");

        when(iniPecConverter.convertoResponsePecToCodeSqsDto(any(), anyString(), anyString())).thenReturn(codeSqsDto);

        ArrayList<CodeSqsDto> codeSqsDtos = new ArrayList<>();
        codeSqsDtos.add(codeSqsDto);

        doNothing().when(sqsRepository).push(codeSqsDtos);

        StepVerifier.create(iniPecService.secondoFlusso()).expectNext(codeSqsDtos).verifyComplete();
    }

    @Test
    void testRecovery(){

        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        batchRequest.setTtl(LocalDateTime.now());
        batchRequest.setRetry(0);
        batchRequest.setCf("taxId");
        batchRequest.setBatchId("batchId");
        batchRequest.setCorrelationId("correlationId");
        batchRequests.add(batchRequest);

        ArrayList<BatchPolling> batchPollings = new ArrayList<>();
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setReservationId("reservationId");
        batchPolling.setStatus("status");
        batchPolling.setBatchId("batchId");
        batchPollings.add(batchPolling);

        when(iniPecBatchRequestRepository.resetBatchIdForRecovery()).thenReturn(Mono.just(batchRequests));

        testPrimoFlusso();

        StepVerifier.create(iniPecService.recoveryPrimoFlusso()).expectNext(batchPollings).verifyComplete();
    }
}

