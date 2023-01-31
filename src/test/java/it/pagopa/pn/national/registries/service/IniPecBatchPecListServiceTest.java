package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

@ExtendWith(SpringExtension.class)
class IniPecBatchPecListServiceTest {
    @InjectMocks
    private IniPecBatchPecListService iniPecBatchPecListService;

    @Mock
    private IniPecBatchPollingRepository iniPecBatchPollingRepository;

    @Mock
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;

    @Mock
    private InfoCamereClient infoCamereClient;

    @Mock
    private InfoCamereConverter infoCamereConverter;

    @Test
    void testBatchPecListRequest() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");
        batchPolling.setStatus("status");

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        batchRequest.setBatchId("batchId");
        batchRequest.setRetry(0);
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId");
        batchRequest2.setCf("cf");
        batchRequest2.setRetry(0);

        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);
        batchRequests.add(batchRequest2);

        Page<BatchRequest> page = Page.create(batchRequests);

        when(iniPecBatchRequestRepository.getBatchRequestByNotBatchIdPageable(new HashMap<>())).thenReturn(Mono.just(page));

        when(iniPecBatchRequestRepository.setNewBatchIdToBatchRequests(anyList(),any())).thenReturn(Mono.just(batchRequests));

        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
        responsePollingIdIniPec.setIdentificativoRichiesta("pollingId");

        when(infoCamereClient.callEServiceRequestId(any())).thenReturn(Mono.just(responsePollingIdIniPec));

        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(any(), eq("pollingId"))).thenReturn(batchPolling);

        when(iniPecBatchPollingRepository.createBatchPolling(batchPolling)).thenReturn(Mono.just(batchPolling));

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());
    }

    @Test
    void testRecoveryPrimoFlusso(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        batchRequest.setBatchId("batchId");
        batchRequest.setRetry(0);
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId");
        batchRequest2.setCf("cf");
        batchRequest2.setRetry(0);

        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);
        batchRequests.add(batchRequest2);

        when(iniPecBatchPollingRepository.createBatchPolling(any())).thenReturn(Mono.just(new BatchPolling()));
        when(iniPecBatchRequestRepository.resetBatchIdForRecovery()).thenReturn(Mono.just(batchRequests));
        testBatchPecListRequest();
        assertDoesNotThrow(() -> iniPecBatchPecListService.recoveryPrimoFlusso());
    }

}
