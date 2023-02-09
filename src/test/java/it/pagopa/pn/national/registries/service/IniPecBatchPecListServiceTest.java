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
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

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
    @MockBean
    private SqsService sqsService;

    @Test
    void testBatchPecRequest() {
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
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        when(batchPollingRepository.create(batchPolling))
                .thenReturn(Mono.just(batchPolling));

        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
        responsePollingIdIniPec.setIdentificativoRichiesta("pollingId");

        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.just(responsePollingIdIniPec));

        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());
        verify(infoCamereClient, times(2)).callEServiceRequestId(any());
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    @DisplayName("Test failure of getBatchRequest with no batch id")
    void testBatchPecRequestDynamoFailure() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(IniPecException.class, () -> iniPecBatchPecListService.batchPecRequest());
    }

    @Test
    void testBatchPecRequestEmpty() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());
        verifyNoInteractions(infoCamereClient);
        verifyNoInteractions(batchPollingRepository);
    }

    @Test
    @DisplayName("Test conditional check failure")
    void testBatchPecRequestConditionalCheckFailure() {
        BatchRequest batchRequest = new BatchRequest();
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    @DisplayName("Test one conditional check failure and one success")
    void testBatchPecRequestConditionalCheckFailureAndOneOk() {
        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCf("cf1");
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCf("cf2");

        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest1, batchRequest2))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        ResponsePollingIdIniPec iniPecResponse = new ResponsePollingIdIniPec();
        iniPecResponse.setIdentificativoRichiesta("pollingId");
        when(infoCamereClient.callEServiceRequestId(any()))
                .thenReturn(Mono.just(iniPecResponse));

        BatchPolling batchPolling = new BatchPolling();
        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);
        when(batchPollingRepository.create(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());

        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    @DisplayName("Test failure of E Service and retry exhausted")
    void testBatchPecRequestEServiceFailureRetryExhausted() {
        BatchRequest batchRequest1 = new BatchRequest();
        BatchRequest batchRequest2 = new BatchRequest();

        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest1, batchRequest2))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));
        when(batchRequestRepository.update(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.update(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.error(exception));

        when(sqsService.push(any(), any()))
                .thenReturn(Mono.error(SqsException.builder().build()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());
        assertDoesNotThrow(() -> iniPecBatchPecListService.batchPecRequest());

        verifyNoInteractions(batchPollingRepository);
        assertEquals(3, batchRequest1.getRetry());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest1.getStatus());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest2.getStatus());
    }

    @Test
    void testRecoveryBatchRequest() {
        BatchRequest batchRequestToRecover = new BatchRequest();
        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of(batchRequestToRecover)));

        testBatchPecRequest();

        assertDoesNotThrow(() -> iniPecBatchPecListService.recoveryBatchRequest());

        verify(batchRequestRepository).update(any());
        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchRequestToRecover.getStatus());
        assertEquals(BatchStatus.NO_BATCH_ID.getValue(), batchRequestToRecover.getBatchId());
    }

    @Test
    void testRecoveryBatchRequestEmpty() {
        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of()));
        assertDoesNotThrow(() -> iniPecBatchPecListService.recoveryBatchRequest());
        verify(batchRequestRepository, never()).setNewBatchIdToBatchRequest(any());
        verifyNoInteractions(sqsService);
        verifyNoInteractions(infoCamereClient);
    }
}
