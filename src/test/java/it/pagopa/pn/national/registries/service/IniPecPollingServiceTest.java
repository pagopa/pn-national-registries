package it.pagopa.pn.national.registries.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.model.SendMessageResult;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

@ContextConfiguration(classes = {IniPecPollingService.class})
@ExtendWith(SpringExtension.class)
class IniPecPollingServiceTest {
    @MockBean
    private IniPecBatchPollingRepository iniPecBatchPollingRepository;

    @MockBean
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;

    @MockBean
    private InfoCamereClient infoCamereClient;

    @MockBean
    private InfoCamereConverter infoCamereConverter;

    @Autowired
    private IniPecPollingService iniPecPollingService;

    @MockBean
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
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.just(responsePecIniPec));

        when(iniPecBatchPollingRepository.updateBatchPolling(batchPolling)).thenReturn(Mono.just(batchPolling));

        when(iniPecBatchRequestRepository.getBatchRequestsToSend("batchId")).thenReturn(Mono.just(batchRequests));

        when(infoCamereConverter.convertoResponsePecToCodeSqsDto(batchRequest, responsePecIniPec)).thenReturn(new CodeSqsDto());

        SendMessageResult sendMessageResult = new SendMessageResult();
        when(sqsService.push(any())).thenReturn(Mono.just(sendMessageResult));

        when(iniPecBatchRequestRepository.setBatchRequestsStatus(batchRequest, BatchStatus.WORKED.getValue())).thenReturn(Mono.just(batchRequest));

        iniPecPollingService.getPecList();

    }

}

