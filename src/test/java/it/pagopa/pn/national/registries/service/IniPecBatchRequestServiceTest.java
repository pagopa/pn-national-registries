package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@TestPropertySource(properties = {
        "pn.national.registries.inipec.batch.request.delay=30000",
        "pn.national-registries.inipec.batch.request.recovery.delay=30000",
        "pn.national-registries.inipec.batch-request-max-retry=3",
        "pn.national-registries.inipec.max-batch-request-size=2"
})
@ContextConfiguration(classes = IniPecBatchRequestService.class)
@ExtendWith(SpringExtension.class)
class IniPecBatchRequestServiceTest {

    @Autowired
    private IniPecBatchRequestService iniPecBatchRequestService;

    @MockitoBean
    private IniPecBatchPollingRepository batchPollingRepository;
    @MockitoBean
    private IniPecBatchRequestRepository batchRequestRepository;
    @MockitoBean
    private InfoCamereClient infoCamereClient;
    @MockitoBean
    private InfoCamereConverter infoCamereConverter;
    @MockitoBean
    private IniPecBatchSqsService iniPecBatchSqsService;
    @MockitoBean
    private ObjectMapper objectMapper;
    @MockitoBean
    private NationalRegistriesConfig nationalRegistriesConfig;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inipec inipecConfig = new NationalRegistriesConfig.Inipec();
        inipecConfig.setBatchRequestMaxRetry(3);
        inipecConfig.setMaxBatchRequestSize(2);
        when(nationalRegistriesConfig.getInipec()).thenReturn(inipecConfig);
    }

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
                .thenReturn(Mono.just(page2));

        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        IniPecBatchResponse iniPecBatchResponse = new IniPecBatchResponse();
        iniPecBatchResponse.setIdentificativoRichiesta("pollingId");

        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.just(iniPecBatchResponse));

        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);

        when(batchPollingRepository.create(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus(anyString(), eq(BatchStatus.TAKEN_CHARGE)))
                .thenReturn(Mono.just(List.of(batchRequest1, batchRequest2)));

        when(batchRequestRepository.update(any(BatchRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());
        verify(infoCamereClient, times(1)).callEServiceRequestId(any());
        verify(batchRequestRepository, times(1)).setNewBatchIdToBatchRequest(same(batchRequest1));
        verify(batchRequestRepository, times(1)).setNewBatchIdToBatchRequest(same(batchRequest2));
        verify(batchPollingRepository, times(1)).create(same(batchPolling));
        verify(batchRequestRepository, times(2)).update(any(BatchRequest.class));
        assertEquals(BatchStatus.WORKING.getValue(), batchRequest1.getStatus());
        assertEquals(BatchStatus.WORKING.getValue(), batchRequest2.getStatus());
    }

    @Test
    @DisplayName("Test failure of getBatchRequest with no batch id")
    void testBatchPecRequestDynamoFailure() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(DigitalAddressException.class, () -> iniPecBatchRequestService.batchPecRequest());
    }

    @Test
    void testBatchPecRequestEmpty() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());
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
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());
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

        IniPecBatchResponse iniPecResponse = new IniPecBatchResponse();
        iniPecResponse.setIdentificativoRichiesta("pollingId");
        when(infoCamereClient.callEServiceRequestId(any()))
                .thenReturn(Mono.just(iniPecResponse));

        BatchPolling batchPolling = new BatchPolling();
        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);
        when(batchPollingRepository.create(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus(anyString(), eq(BatchStatus.TAKEN_CHARGE)))
                .thenReturn(Mono.just(List.of(batchRequest2)));
        when(batchRequestRepository.update(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());

        verify(infoCamereClient, times(1)).callEServiceRequestId(any());
        verify(batchRequestRepository, times(1)).setNewBatchIdToBatchRequest(same(batchRequest1));
        verify(batchRequestRepository, times(1)).setNewBatchIdToBatchRequest(same(batchRequest2));
        verify(batchRequestRepository, times(1)).update(same(batchRequest2));
        verify(batchRequestRepository, never()).update(same(batchRequest1));
        assertEquals(BatchStatus.WORKING.getValue(), batchRequest2.getStatus());
    }

    @Test
    @DisplayName("Test failure of E Service")
    void testBatchPecRequestEServiceFailure() {
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

        when(iniPecBatchSqsService.sendListToDlqQueue(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());

        verifyNoInteractions(batchPollingRepository);
        verifyNoInteractions(iniPecBatchSqsService);
        assertEquals(1, batchRequest.getRetry());
        assertEquals(BatchStatus.TAKEN_CHARGE.getValue(), batchRequest.getStatus());
        assertNull(batchRequest.getSendStatus());
    }

    @Test
    @DisplayName("Test failure of E Service and retry exhausted")
    void testBatchPecRequestEServiceFailureRetryExhausted() {
        BatchRequest batchRequest = new BatchRequest();

        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));
        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getMessage()).thenReturn("message");
        when(infoCamereClient.callEServiceRequestId(isNotNull()))
                .thenReturn(Mono.error(exception));

        when(iniPecBatchSqsService.sendListToDlqQueue(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest());

        verifyNoInteractions(batchPollingRepository);
        assertEquals(3, batchRequest.getRetry());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest.getStatus());
    }

    @Test
    void testRecoveryBatchRequest() {
        BatchRequest batchRequestToRecover1 = new BatchRequest();
        BatchRequest batchRequestToRecover2 = new BatchRequest();

        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of(batchRequestToRecover1, batchRequestToRecover2)));
        when(batchRequestRepository.update(same(batchRequestToRecover1)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        when(batchRequestRepository.update(same(batchRequestToRecover2)))
                .thenReturn(Mono.just(batchRequestToRecover2));

        assertDoesNotThrow(() -> iniPecBatchRequestService.recoveryBatchRequest());

        verify(batchRequestRepository, times(1)).update(same(batchRequestToRecover1));
        verify(batchRequestRepository, times(1)).update(same(batchRequestToRecover2));

        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchRequestToRecover2.getStatus());
        assertEquals(BatchStatus.NO_BATCH_ID.getValue(), batchRequestToRecover2.getBatchId());
    }

    @Test
    void testRecoveryBatchRequestEmpty() {
        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of()));
        assertDoesNotThrow(() -> iniPecBatchRequestService.recoveryBatchRequest());
        verify(batchRequestRepository, never()).setNewBatchIdToBatchRequest(any());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    void testHandleRetryAndCheckDlq() {
        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCorrelationId("correlationId1");
        batchRequest1.setCf("cf1");
        batchRequest1.setRetry(2);

        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId2");
        batchRequest2.setCf("cf2");
        batchRequest2.setRetry(2);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getMessage()).thenReturn("E Service error");

        when(batchRequestRepository.update(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.update(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        when(iniPecBatchSqsService.sendListToDlqQueue(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchRequestService.handleRetryAndCheckDlq(
                List.of(batchRequest1, batchRequest2), exception, "batchId").block());

        assertEquals(3, batchRequest1.getRetry());
        assertEquals(3, batchRequest2.getRetry());

        assertEquals(BatchStatus.ERROR.getValue(), batchRequest1.getStatus());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest2.getStatus());

        verify(iniPecBatchSqsService, times(1)).sendListToDlqQueue(anyList());
    }

    @Test
    void testHandleRetryAndCheckDlqRetryNotExhausted() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        batchRequest.setRetry(1); // Retry < maxRetry (3)

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);

        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        assertDoesNotThrow(() -> iniPecBatchRequestService.handleRetryAndCheckDlq(
                List.of(batchRequest), exception, "batchId").block());

        assertEquals(2, batchRequest.getRetry());

        assertNotEquals(BatchStatus.ERROR.getValue(), batchRequest.getStatus());

        verifyNoInteractions(iniPecBatchSqsService);
    }

    @Test
    void testHandleRetryAndCheckDlqWithBadRequest() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        batchRequest.setRetry(0);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        when(iniPecBatchSqsService.sendListToDlqQueue(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchRequestService.handleRetryAndCheckDlq(
                List.of(batchRequest), exception, "batchId").block());

        assertEquals(BatchStatus.ERROR.getValue(), batchRequest.getStatus());
        assertEquals(1, batchRequest.getRetry());

        verify(iniPecBatchSqsService, times(1)).sendListToDlqQueue(anyList());
    }
}
