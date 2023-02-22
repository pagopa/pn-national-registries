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
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
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
        "pn.national.registries.inipec.batch.polling.delay=30000",
        "pn.national-registries.inipec.batch.polling.recovery.delay=30000",
        "pn.national-registries.inipec.batch.polling.max-retry=3"
})
@ContextConfiguration(classes = IniPecBatchPollingService.class)
@ExtendWith(SpringExtension.class)
class IniPecBatchPollingServiceTest {

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

    @Autowired
    private IniPecBatchPollingService iniPecBatchPollingService;

    @Test
    void testBatchPecPolling() {
        /*
        Questo test simula il flusso con tre polling recuperati da query separate, di cui:
            * il primo polling ha due batch request
            * il secondo polling non ha batch request
            * il terzo polling ha una batch request
        Tutti e tre i polling vengono eseguiti con successo.
         */
        BatchPolling batchPolling1 = new BatchPolling();
        batchPolling1.setBatchId("batchId1");
        batchPolling1.setPollingId("pollingId1");

        BatchPolling batchPolling2 = new BatchPolling();
        batchPolling2.setBatchId("batchId2");
        batchPolling2.setPollingId("pollingId2");

        BatchPolling batchPolling3 = new BatchPolling();
        batchPolling3.setBatchId("batchId3");
        batchPolling3.setPollingId("pollingId3");

        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCorrelationId("correlationId1");
        batchRequest1.setBatchId("batchId1");
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId2");
        batchRequest2.setBatchId("batchId1");
        BatchRequest batchRequest3 = new BatchRequest();
        batchRequest3.setCorrelationId("correlationId3");
        batchRequest3.setBatchId("batchId3");

        Page<BatchPolling> page1 = Page.create(List.of(batchPolling1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<BatchPolling> page2 = Page.create(List.of(batchPolling2, batchPolling3));

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling1)))
                .thenReturn(Mono.just(batchPolling1));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling2)))
                .thenReturn(Mono.just(batchPolling2));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling3)))
                .thenReturn(Mono.just(batchPolling3));

        IniPecPollingResponse iniPecPollingResponse1 = new IniPecPollingResponse();
        iniPecPollingResponse1.setIdentificativoRichiesta("correlationId1");
        iniPecPollingResponse1.setElencoPec(Collections.emptyList());
        IniPecPollingResponse iniPecPollingResponse2 = new IniPecPollingResponse();
        iniPecPollingResponse2.setIdentificativoRichiesta("correlationId2");
        iniPecPollingResponse2.setElencoPec(Collections.emptyList());
        IniPecPollingResponse iniPecPollingResponse3 = new IniPecPollingResponse();
        iniPecPollingResponse3.setIdentificativoRichiesta("correlationId3");
        iniPecPollingResponse3.setElencoPec(Collections.emptyList());

        when(infoCamereClient.callEServiceRequestPec("pollingId1"))
                .thenReturn(Mono.just(iniPecPollingResponse1));
        when(infoCamereClient.callEServiceRequestPec("pollingId2"))
                .thenReturn(Mono.just(iniPecPollingResponse2));
        when(infoCamereClient.callEServiceRequestPec("pollingId3"))
                .thenReturn(Mono.just(iniPecPollingResponse3));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId1", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest1, batchRequest2)));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId2", BatchStatus.WORKING))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId3", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest3)));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any()))
                .thenReturn(codeSqsDto);

        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        when(batchPollingRepository.update(same(batchPolling1)))
                .thenReturn(Mono.just(batchPolling1));
        when(batchPollingRepository.update(same(batchPolling2)))
                .thenReturn(Mono.just(batchPolling2));
        when(batchPollingRepository.update(same(batchPolling3)))
                .thenReturn(Mono.just(batchPolling3));
        when(batchRequestRepository.update(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.update(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));
        when(batchRequestRepository.update(same(batchRequest3)))
                .thenReturn(Mono.just(batchRequest3));

        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());

        assertEquals(BatchStatus.WORKED.getValue(), batchPolling1.getStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchPolling2.getStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchPolling3.getStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest1.getStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest1.getSendStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest2.getStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest2.getSendStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest3.getStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest3.getSendStatus());
        verify(infoCamereConverter).convertResponsePecToCodeSqsDto(same(batchRequest1), same(iniPecPollingResponse1));
        verify(infoCamereConverter).convertResponsePecToCodeSqsDto(same(batchRequest2), same(iniPecPollingResponse1));
        verify(infoCamereConverter).convertResponsePecToCodeSqsDto(same(batchRequest3), same(iniPecPollingResponse3));
        verify(iniPecBatchSqsService).batchSendToSqs(List.of(batchRequest1, batchRequest2));
        verify(iniPecBatchSqsService).batchSendToSqs(List.of(batchRequest3));
    }

    @Test
    void testBatchPecPolling2() {
        /*
        Questo test simula il flusso con due polling recuperati da una query, di cui:
            * il primo polling va in errore durante la call all'E Service
            * il secondo polling va in successo
        Viene inviato un messaggio di successo sulla coda.
         */
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setBatchId("batchId");

        BatchPolling pollingInFailure = new BatchPolling();

        Page<BatchPolling> page = Page.create(List.of(pollingInFailure, batchPolling));

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(page));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(pollingInFailure)))
                .thenReturn(Mono.just(pollingInFailure));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setIdentificativoRichiesta("correlationId");
        iniPecPollingResponse.setElencoPec(Collections.emptyList());

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(infoCamereClient.callEServiceRequestPec(any()))
                .thenReturn(Mono.error(exception))
                .thenReturn(Mono.just(iniPecPollingResponse));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest)));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any()))
                .thenReturn(codeSqsDto);

        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        when(batchPollingRepository.update(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));
        when(batchPollingRepository.update(same(pollingInFailure)))
                .thenReturn(Mono.just(pollingInFailure));
        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());

        assertEquals(BatchStatus.WORKED.getValue(), batchPolling.getStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest.getStatus());
        assertEquals(1, pollingInFailure.getRetry());
        verify(infoCamereConverter).convertResponsePecToCodeSqsDto(same(batchRequest), same(iniPecPollingResponse));
        verify(iniPecBatchSqsService).batchSendToSqs(List.of(batchRequest));
    }

    @Test
    @DisplayName("Test failure of getBatchPolling with no reservationId and status not worked")
    void testBatchPecPollingDynamoFailure() {
        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(IniPecException.class, () -> iniPecBatchPollingService.batchPecPolling());
    }

    @Test
    void testBatchPecPollingEmpty() {
        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    @DisplayName("Test conditional check failure")
    void testBatchPecPollingConditionalCheckFailure() {
        BatchPolling batchPolling = new BatchPolling();
        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchPolling))));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }

    @Test
    @DisplayName("Test conditional check failure and one success")
    void testBatchPecPollingConditionalCheckFailureAndOneOk() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setBatchId("batchId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());

        Page<BatchPolling> page = Page.create(List.of(new BatchPolling(), batchPolling));

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(page));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(any()))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()))
                .thenReturn(Mono.just(batchPolling));

        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setIdentificativoRichiesta("correlationId");
        iniPecPollingResponse.setElencoPec(Collections.emptyList());

        when(infoCamereClient.callEServiceRequestPec("pollingId"))
                .thenReturn(Mono.just(iniPecPollingResponse));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest)));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any()))
                .thenReturn(codeSqsDto);

        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        when(batchPollingRepository.update(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));
        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());

        assertEquals(BatchStatus.WORKED.getValue(), batchPolling.getStatus());
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest.getStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        verify(iniPecBatchSqsService).batchSendToSqs(List.of(batchRequest));
    }

    @Test
    @DisplayName("Test failure of E Service and retry")
    void testBatchPecPollingRetry() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchPolling))));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));
        when(batchPollingRepository.update(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(infoCamereClient.callEServiceRequestPec("pollingId"))
                .thenReturn(Mono.error(exception));

        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());

        verifyNoInteractions(batchRequestRepository);
        verifyNoInteractions(iniPecBatchSqsService);
        assertEquals(1, batchPolling.getRetry());
        assertEquals(BatchStatus.WORKING.getValue(), batchPolling.getStatus());
        assertNotNull(batchPolling.getLastReserved());
        assertNotNull(batchPolling.getReservationId());
    }

    @Test
    @DisplayName("Test failure of E Service and retry exhausted")
    void testBatchPecPollingRetryExhaustedAndSqsFailure() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setBatchId("batchId");

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchPolling))));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));
        when(batchPollingRepository.update(same(batchPolling)))
                .thenReturn(Mono.just(batchPolling));

        BatchRequest batchRequest1 = new BatchRequest();
        BatchRequest batchRequest2 = new BatchRequest();

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest1, batchRequest2)));
        when(batchRequestRepository.update(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));
        when(batchRequestRepository.update(same(batchRequest2)))
                .thenReturn(Mono.just(batchRequest2));

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(infoCamereClient.callEServiceRequestPec("pollingId"))
                .thenReturn(Mono.error(exception));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertIniPecRequestToSqsDto(any(), any()))
                .thenReturn(codeSqsDto);
        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());
        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());
        assertDoesNotThrow(() -> iniPecBatchPollingService.batchPecPolling());

        assertEquals(3, batchPolling.getRetry());
        assertEquals(BatchStatus.ERROR.getValue(), batchPolling.getStatus());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest1.getStatus());
        assertEquals(BatchStatus.ERROR.getValue(), batchRequest2.getStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest1.getSendStatus());
        assertEquals(BatchStatus.NOT_SENT.getValue(), batchRequest2.getSendStatus());
        verify(iniPecBatchSqsService).batchSendToSqs(List.of(batchRequest1, batchRequest2));
    }

    @Test
    void testRecoveryBatchPolling() {
        BatchPolling batchPollingToRecover1 = new BatchPolling();
        BatchPolling batchPollingToRecover2 = new BatchPolling();

        when(batchPollingRepository.getBatchPollingToRecover())
                .thenReturn(Mono.just(List.of(batchPollingToRecover1, batchPollingToRecover2)));
        when(batchPollingRepository.resetBatchPollingForRecovery(same(batchPollingToRecover1)))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPollingToRecover2)))
                .thenReturn(Mono.just(batchPollingToRecover2));

        testBatchPecPolling();

        assertDoesNotThrow(() -> iniPecBatchPollingService.recoveryBatchPolling());

        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchPollingToRecover2.getStatus());
        assertNull(batchPollingToRecover2.getReservationId());
    }

    @Test
    void testRecoveryBatchPollingEmpty() {
        when(batchPollingRepository.getBatchPollingToRecover())
                .thenReturn(Mono.just(Collections.emptyList()));
        assertDoesNotThrow(() -> iniPecBatchPollingService.recoveryBatchPolling());
        verify(batchPollingRepository, never()).setNewReservationIdToBatchPolling(any());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }
}
