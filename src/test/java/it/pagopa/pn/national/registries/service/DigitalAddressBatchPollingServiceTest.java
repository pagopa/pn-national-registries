package it.pagopa.pn.national.registries.service;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;

import java.time.OffsetDateTime;
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
        "pn.national-registries.inipec.batch.polling.max-retry=3",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=24"
})
@ContextConfiguration(classes = DigitalAddressBatchPollingService.class)
@ExtendWith(SpringExtension.class)
class DigitalAddressBatchPollingServiceTest {

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
    private InadService inadService;
    @Autowired
    private DigitalAddressBatchPollingService digitalAddressBatchPollingService;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private IniPecBatchRequestService iniPecBatchRequestService;

    @Test
    void testBatchPecPollingIncrementAndCheckRetryError() {
        BatchPolling batchPolling1 = new BatchPolling();
        batchPolling1.setBatchId("batchId1");
        batchPolling1.setPollingId("pollingId1");
        batchPolling1.setRetry(2);
        batchPolling1.setInProgressRetry(2);

        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCorrelationId("correlationId1");
        batchRequest1.setBatchId("batchId1");

        Page<BatchPolling> page1 = Page.create(List.of(batchPolling1), new HashMap<>());

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1));

        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling1)))
                .thenReturn(Mono.just(batchPolling1));

        PnNationalRegistriesException pnNationalRegistriesException = mock(PnNationalRegistriesException.class);

        when(infoCamereClient.callEServiceRequestPec("pollingId1")).thenReturn(Mono.error(pnNationalRegistriesException));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId1", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest1)));


        CodeSqsDto codeSqsDto = mock(CodeSqsDto.class);
        when(codeSqsDto.getError()).thenReturn("error");
        when(infoCamereConverter.convertIniPecRequestToSqsDto(any(), any()))
                .thenReturn(codeSqsDto);

        when(iniPecBatchSqsService.batchSendToSqs(anyList()))
                .thenReturn(Mono.empty().then());

        when(batchPollingRepository.update(same(batchPolling1)))
                .thenReturn(Mono.just(batchPolling1));
        when(batchRequestRepository.update(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
    }

    @Test
    void testBatchPecPollingIncrementAndCheckRetryNotError() {
        BatchPolling batchPolling1 = new BatchPolling();
        batchPolling1.setBatchId("batchId1");
        batchPolling1.setPollingId("pollingId1");
        batchPolling1.setRetry(2);
        batchPolling1.setInProgressRetry(2);

        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCorrelationId("correlationId1");
        batchRequest1.setBatchId("batchId1");
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId2");
        batchRequest2.setBatchId("batchId1");
        BatchRequest batchRequest3 = new BatchRequest();
        batchRequest3.setCorrelationId("correlationId3");
        batchRequest3.setBatchId("batchId3");

        Page<BatchPolling> page1 = Page.create(List.of(batchPolling1), new HashMap<>());

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1));

        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling1)))
                .thenReturn(Mono.just(batchPolling1));

        IniPecPollingResponse iniPecPollingResponse1 = new IniPecPollingResponse();
        iniPecPollingResponse1.setIdentificativoRichiesta("correlationId1");
        iniPecPollingResponse1.setElencoPec(Collections.emptyList());
        iniPecPollingResponse1.setDescription("List PEC in progress");

        when(infoCamereClient.callEServiceRequestPec("pollingId1")).thenReturn(Mono.just(iniPecPollingResponse1));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError(any())).thenReturn(true);
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId1", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest1, batchRequest2)));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId2", BatchStatus.WORKING))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId3", BatchStatus.WORKING))
                .thenReturn(Mono.just(List.of(batchRequest3)));

        when(batchPollingRepository.update(any()))
                .thenReturn(Mono.just(batchPolling1));

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
    }
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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
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
        DigitalAddress digitalAddress = new DigitalAddress();
        digitalAddress.setAddress("address@pec.it");

        DigitalAddress digitalAddress2 = new DigitalAddress();
        digitalAddress2.setAddress("invalid_pec");

        codeSqsDto.setDigitalAddress(List.of(digitalAddress, digitalAddress2));

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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

    }

    @Test
    void testBatchPecPolling3() {
        /*
        Questo test simula il flusso con un polling che riceve una risposta di Elaborazione in corso da parte di infocamere
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
        iniPecPollingResponse.setCode("WSPA_ERR_05");
        iniPecPollingResponse.setDescription("List PEC in progress");
        iniPecPollingResponse.setTimestamp(OffsetDateTime.now().toString());
        iniPecPollingResponse.setAppName("wspa-pedf");

        when(infoCamereClient.callEServiceRequestPec(any()))
                .thenReturn(Mono.just(iniPecPollingResponse));

        when(infoCamereConverter.checkIfResponseIsInfoCamereError((IniPecPollingResponse) any())).thenReturn(true);

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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

    }

    @Test
    @DisplayName("Test failure of getBatchPolling with no reservationId and status not worked")
    void testBatchPecPollingDynamoFailure() {
        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.empty());
        assertThrows(DigitalAddressException.class, () -> digitalAddressBatchPollingService.batchPecPolling());
    }

    @Test
    void testBatchPecPollingEmpty() {
        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
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
        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

        verifyNoInteractions(batchRequestRepository);
        verifyNoInteractions(iniPecBatchSqsService);
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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());
        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

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

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.recoveryBatchPolling());

        assertEquals(BatchStatus.NOT_WORKED.getValue(), batchPollingToRecover2.getStatus());
        assertNull(batchPollingToRecover2.getReservationId());
    }

    @Test
    void testRecoveryBatchPollingEmpty() {
        when(batchPollingRepository.getBatchPollingToRecover())
                .thenReturn(Mono.just(Collections.emptyList()));
        assertDoesNotThrow(() -> digitalAddressBatchPollingService.recoveryBatchPolling());
        verify(batchPollingRepository, never()).setNewReservationIdToBatchPolling(any());
        verifyNoInteractions(iniPecBatchSqsService);
        verifyNoInteractions(infoCamereClient);
    }
}
