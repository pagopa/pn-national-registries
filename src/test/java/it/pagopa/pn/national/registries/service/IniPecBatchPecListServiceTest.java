package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@TestPropertySource(properties = {
        "pn.national.registries.inipec.batch.request.delay=30000",
        "pn.national-registries.inipec.batch.request.recovery.delay=30000",
        "pn.national-registries.inipec.batch.request.max-retry=3"
})
@ContextConfiguration(classes = IniPecBatchPecListService.class)
@ExtendWith(SpringExtension.class)
class IniPecBatchPecListServiceTest {

    @Autowired
    private IniPecBatchPecListService iniPecBatchPecListService;

    @MockBean
    private IniPecBatchPollingRepository batchPollingRepository;
    @MockBean
    private IniPecBatchRequestRepository batchRequestRepository;
    @MockBean
    private InfoCamereClient infoCamereClient;
    @MockBean
    private InfoCamereConverter infoCamereConverter;

    @Test
    void testBatchPecListRequest() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");

        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCorrelationId("correlationId");
        batchRequest1.setCf("cf");

        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId");
        batchRequest2.setCf("cf");

        Page<BatchRequest> page1 = Page.create(List.of(batchRequest1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<BatchRequest> page2 = Page.create(List.of(batchRequest2));

        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        when(batchPollingRepository.createBatchPolling(batchPolling))
                .thenReturn(Mono.just(batchPolling));

        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
        responsePollingIdIniPec.setIdentificativoRichiesta("pollingId");

        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.just(responsePollingIdIniPec));

        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());
        verify(infoCamereClient, times(2)).callEServiceRequestId(any());
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    @DisplayName("Test failure of getBatchRequest with no batch id")
    void testBatchPecListRequestDynamoFailure() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(IniPecException.class, () -> iniPecBatchPecListService.batchPecListRequest());
    }

    @Test
    @DisplayName("Test conditional check failure")
    void testBatchPecListRequestConditionalCheckFailure() {
        BatchRequest batchRequest = new BatchRequest();
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    @DisplayName("Test failure of E Service and retry exhausted")
    void testBatchPecListRequestEServiceFailureRetryExhausted() {
        BatchRequest batchRequest = new BatchRequest();

        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));
        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.error(exception));

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecListRequest());

        verifyNoInteractions(batchPollingRepository);
        assertEquals(3, batchRequest.getRetry());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest.getStatus());
    }

    @Test
    void testRecoveryPrimoFlusso() {
        BatchRequest batchRequestToRecover = new BatchRequest();
        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of(batchRequestToRecover)));

        testBatchPecListRequest();

        assertDoesNotThrow(() -> iniPecBatchPecListService.recoveryPrimoFlusso());

        verify(batchRequestRepository).update(any());
        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchRequestToRecover.getStatus());
        assertEquals(BatchStatus.NO_BATCH_ID.getValue(), batchRequestToRecover.getBatchId());
    }
}
