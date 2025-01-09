package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
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
        "pn.national-registries.inipec.batch.request.max-retry=3",
        "pn.national-registries.inipec.max.batch.request.size=1"
})
@ContextConfiguration(classes = IniPecBatchRequestService.class)
@ExtendWith(SpringExtension.class)
class IniPecBatchRequestServiceTest {

    @Autowired
    private IniPecBatchRequestService iniPecBatchRequestService;

    @MockBean
    private IniPecBatchPollingRepository batchPollingRepository;
    @MockBean
    private IniPecBatchRequestRepository batchRequestRepository;
    @MockBean
    private InfoCamereClient infoCamereClient;
    @MockBean
    private InfoCamereConverter infoCamereConverter;
    @MockBean
    private IniPecBatchSqsService iniPecBatchSqsService;
    @MockBean
    private ObjectMapper objectMapper;

    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;

    @BeforeEach
    public void setup() {
        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
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
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        when(batchPollingRepository.create(batchPolling))
                .thenReturn(Mono.just(batchPolling));

        IniPecBatchResponse iniPecBatchResponse = new IniPecBatchResponse();
        iniPecBatchResponse.setIdentificativoRichiesta("pollingId");

        when(infoCamereClient.callEServiceRequestId(isNotNull(), any()))
                .thenReturn(Mono.just(iniPecBatchResponse));

        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));
        verify(infoCamereClient, times(2)).callEServiceRequestId(any(), any());
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    @DisplayName("Test failure of getBatchRequest with no batch id")
    void testBatchPecRequestDynamoFailure() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(DigitalAddressException.class, () -> iniPecBatchRequestService.batchPecRequest(logEvent));
    }

    @Test
    void testBatchPecRequestEmpty() {
        when(batchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));
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
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));
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
        when(infoCamereClient.callEServiceRequestId(any(), any()))
                .thenReturn(Mono.just(iniPecResponse));

        BatchPolling batchPolling = new BatchPolling();
        when(infoCamereConverter.createBatchPollingByBatchIdAndPollingId(anyString(), eq("pollingId")))
                .thenReturn(batchPolling);
        when(batchPollingRepository.create(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));

        verify(batchRequestRepository, never()).update(any());
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
        when(infoCamereClient.callEServiceRequestId(isNotNull(), any()))
                .thenReturn(Mono.error(exception));

        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));

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
        when(infoCamereClient.callEServiceRequestId(isNotNull(), any()))
                .thenReturn(Mono.error(exception));

        when(iniPecBatchSqsService.sendListToDlqQueue(anyList()))
                .thenReturn(Mono.empty().then());

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertIniPecRequestToSqsDto(same(batchRequest), anyString()))
                .thenReturn(codeSqsDto);

        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));
        assertDoesNotThrow(() -> iniPecBatchRequestService.batchPecRequest(logEvent));

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
        when(batchRequestRepository.resetBatchRequestForRecovery(same(batchRequestToRecover1)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        when(batchRequestRepository.resetBatchRequestForRecovery(same(batchRequestToRecover2)))
                .thenReturn(Mono.just(batchRequestToRecover2));

        testBatchPecRequest();

        assertDoesNotThrow(() -> iniPecBatchRequestService.recoveryBatchRequest(logEvent));

        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchRequestToRecover2.getStatus());
        assertEquals(BatchStatus.NO_BATCH_ID.getValue(), batchRequestToRecover2.getBatchId());
    }

    @Test
    void testRecoveryBatchRequestEmpty() {
        when(batchRequestRepository.getBatchRequestToRecovery())
                .thenReturn(Mono.just(List.of()));
        assertDoesNotThrow(() -> iniPecBatchRequestService.recoveryBatchRequest(logEvent));
        verify(batchRequestRepository, never()).setNewBatchIdToBatchRequest(any());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }

}
