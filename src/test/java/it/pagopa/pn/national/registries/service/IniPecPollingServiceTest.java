package it.pagopa.pn.national.registries.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(SpringExtension.class)
class IniPecPollingServiceTest {
    @Mock
    private IniPecBatchPollingRepository iniPecBatchPollingRepository;

    @Mock
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;

    @Mock
    private InfoCamereClient infoCamereClient;

    @Mock
    private InfoCamereConverter infoCamereConverter;

    @InjectMocks
    private IniPecPollingService iniPecPollingService;

    @Mock
    private SqsService sqsService;

    @Test
    void testGetPecList() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");
        batchPolling.setStatus("status");
        ArrayList<BatchPolling> batchPollings = new ArrayList<>();
        batchPollings.add(batchPolling);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);

        Page<BatchPolling> page = Page.create(batchPollings);
        when(iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork(new HashMap<>())).thenReturn(Mono.just(page));

        when(iniPecBatchPollingRepository.setReservationIdToAndStatusWorkingBatchPolling(anyList(),anyString())).thenReturn(Mono.just(batchPollings));

        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        responsePecIniPec.setIdentificativoRichiesta("correlationId");
        responsePecIniPec.setElencoPec(new ArrayList<>());
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.just(responsePecIniPec));

        when(iniPecBatchPollingRepository.updateBatchPolling(batchPolling)).thenReturn(Mono.just(batchPolling));

        when(iniPecBatchRequestRepository.getBatchRequestsToSend("batchId")).thenReturn(Mono.just(batchRequests));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setTaxId("taxId");
        when(infoCamereConverter.convertoResponsePecToCodeSqsDto(batchRequest, responsePecIniPec)).thenReturn(codeSqsDto);

        SendMessageResponse sendMessageResult = SendMessageResponse.builder().build();
        when(sqsService.push(any(),any())).thenReturn(Mono.just(sendMessageResult));

        when(iniPecBatchRequestRepository.setBatchRequestsStatus(batchRequest, BatchStatus.WORKED.getValue())).thenReturn(Mono.just(batchRequest));

        iniPecPollingService.getPecList();

        Assertions.assertEquals("WORKED", batchPolling.getStatus());

    }

}

