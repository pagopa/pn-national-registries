package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = DigitalAddressBatchPollingService.class)
@ExtendWith(SpringExtension.class)
class DigitalAddressBatchPollingServiceTest {

    @MockitoBean
    private IniPecBatchPollingRepository batchPollingRepository;
    @MockitoBean
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    @MockitoBean
    private IniPecBatchRequestRepository batchRequestRepository;
    @MockitoBean
    private InfoCamereClient infoCamereClient;
    @MockitoBean
    private InfoCamereConverter infoCamereConverter;
    @MockitoBean
    private IniPecBatchSqsService iniPecBatchSqsService;

    @MockitoBean
    private InadService inadService;

    @MockitoBean
    private IpaService ipaService;

    @Autowired
    private DigitalAddressBatchPollingService digitalAddressBatchPollingService;

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
    private IniPecBatchRequestService iniPecBatchRequestService;

    @MockitoBean
    private GatewayUtils gatewayUtils;

    @MockitoBean
    private NationalRegistriesConfig nationalRegistriesConfig;
    @MockitoBean
    private DigitalAddressUtils digitalAddressUtils;

    @BeforeEach
    void setup(){
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setBatchRequestPkSeparator("~");
        inipec.setBatchPollingMaxRetry(3);
        inipec.setBatchPollingInProgressMaxRetry(24);
        when(nationalRegistriesConfig.getInipec()).thenReturn(inipec);
    }

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

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId1", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest1))));


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
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((IniPecPollingResponse) any())).thenReturn(true);
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId1", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest1, batchRequest2))));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId2", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId3", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest3))));

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
        batchRequest1.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        batchRequest1.setRecipientType(PG.name());

        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setCorrelationId("correlationId2");
        batchRequest2.setBatchId("batchId1");
        batchRequest2.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        batchRequest2.setRecipientType(PG.name());


        BatchRequest batchRequest3 = new BatchRequest();
        batchRequest3.setCorrelationId("correlationId3");
        batchRequest3.setBatchId("batchId3");
        batchRequest3.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        batchRequest3.setRecipientType(PG.name());


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

        when(infoCamereConverter.checkIfResponseIsInfoCamereError(any(IniPecPollingResponse.class))).thenReturn(false);

        CodeSqsDto emptySqs = new CodeSqsDto();
        emptySqs.setDigitalAddress(Collections.emptyList());
        emptySqs.setError(null);
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any())).thenReturn(emptySqs);

        Map<String, AttributeValue> firstLastKey = new HashMap<>();
        firstLastKey.put("k", AttributeValue.builder().s("v").build());

        Page<BatchRequest> batch1Page1 = Page.create(List.of(batchRequest1), firstLastKey);
        Page<BatchRequest> batch1Page2 = Page.create(List.of(batchRequest2));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus(eq("batchId1"), eq(BatchStatus.WORKING), anyMap()))
                .thenReturn(Mono.just(batch1Page1))
                .thenReturn(Mono.just(batch1Page2));

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId2", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId3", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest3))));

        GetDigitalAddressINADOKDto inadResp = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("inad@pec.it");
        inadResp.setDigitalAddress(digitalAddressDto);
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(inadResp));

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

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest1.getSendStatus());
        assertNotNull(batchRequest1.getLastReserved());

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest2.getSendStatus());
        assertNotNull(batchRequest2.getLastReserved());

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest3.getSendStatus());
        assertNotNull(batchRequest3.getLastReserved());
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

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        DigitalAddress digitalAddress = new DigitalAddress();
        digitalAddress.setAddress("address@pec.it");

        DigitalAddress digitalAddress2 = new DigitalAddress();
        digitalAddress2.setAddress("invalid_pec");

        codeSqsDto.setDigitalAddress(List.of(digitalAddress, digitalAddress2));
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any())).thenReturn(codeSqsDto);

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

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getLastReserved());
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

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));

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

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));

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

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getLastReserved());
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

        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest1, batchRequest2))));
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

    @Test
    void testStatoImpresaNullWithPec() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("testCorrelationId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        Pec pec = new Pec();
        pec.setPecImpresa("pec@pec.it");
        pec.setStatoImpresa(null);

        BatchStatus status = BatchStatus.valueOf(batchRequest.getStatus());
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        DigitalAddress address = new DigitalAddress();
        address.setAddress("pec@pec.it");
        codeSqsDto.setDigitalAddress(List.of(address));
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any())).thenReturn(codeSqsDto);
        when(infoCamereConverter.updateBatchRequestFields(any(BatchRequest.class), any(BatchStatus.class), any(LocalDateTime.class), any(CodeSqsDto.class)))
                .thenAnswer(inv -> Mono.just(batchRequest));

        BatchRequest result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, status, LocalDateTime.now()).block();
        assertSame(batchRequest, result);
    }

    @Test
    void testStatoImpresaNullWithoutPec() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("testCorrelationId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        Pec pec = new Pec();
        pec.setPecImpresa("2026-08-10T00:00:00Z");
        pec.setStatoImpresa(null);
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.empty());
        BatchStatus status = BatchStatus.valueOf(batchRequest.getStatus());
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        DigitalAddress address = new DigitalAddress();
        address.setAddress("2026-08-10T00:00:00Z");
        codeSqsDto.setDigitalAddress(List.of(address));
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(), any())).thenReturn(codeSqsDto);
        when(infoCamereConverter.updateBatchRequestFields(any(BatchRequest.class), any(BatchStatus.class), any(LocalDateTime.class), any(CodeSqsDto.class)))
                .thenAnswer(inv -> Mono.just(batchRequest));

        BatchRequest result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, status, LocalDateTime.now()).block();
        assertSame(batchRequest, result);
    }

    @Test
    void testStatoImpresaER() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("testBatchId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        Pec pec = new Pec();
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);

        BatchStatus status = BatchStatus.valueOf(batchRequest.getStatus());

        when(iniPecBatchRequestService.handleRetryAndCheckDlq(anyList(), isNull(), eq("testBatchId")))
                .thenReturn(Mono.empty());

        var result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, status, LocalDateTime.now()).block();
        assertNull(result);
        verify(iniPecBatchRequestService, times(1))
                .handleRetryAndCheckDlq(anyList(), isNull(), eq("testBatchId"));
    }

    @Test
    void testStatoImpresaERHandleRetryFailure() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("testBatchId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        Pec pec = new Pec();
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);

        BatchStatus status = BatchStatus.valueOf(batchRequest.getStatus());

        when(iniPecBatchRequestService.handleRetryAndCheckDlq(anyList(), isNull(), eq("testBatchId")))
                .thenReturn(Mono.error(new RuntimeException("retry-failed")));

        Mono<BatchRequest> mono = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, status, LocalDateTime.now());

        assertThrows(RuntimeException.class, mono::block);
        verify(iniPecBatchRequestService, times(1))
                .handleRetryAndCheckDlq(anyList(), isNull(), eq("testBatchId"));
    }

    @ParameterizedTest
    @EnumSource(value = Pec.StatoImpresaEnum.class, names = {"ND", "NF"})
    void testStatoImpresaNdNf(Pec.StatoImpresaEnum statoImpresa) {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("testBatchId");
        batchRequest.setCorrelationId("testCorrelationId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        batchRequest.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        batchRequest.setCf("32565856321");
        Pec pec = new Pec();
        pec.setStatoImpresa(statoImpresa);

        BatchStatus status = BatchStatus.valueOf(batchRequest.getStatus());

        GetDigitalAddressINADOKDto inadResp = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("inad@pec.it");
        inadResp.setDigitalAddress(digitalAddressDto);

        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.just(inadResp));

        BatchRequest result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, status, LocalDateTime.now()).block();

        assertSame(batchRequest, result);
        String resultingStatus = result != null ? result.getStatus() : null;
        assertNotNull(result);
        assertNotEquals(BatchStatus.TAKEN_CHARGE.getValue(), resultingStatus);
        verifyNoInteractions(iniPecBatchRequestService);
    }

    @Test
    void testStatoImpresaNullWithEmptyPecListFallbackToInad() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf());
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(validInadResponse()));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(BatchStatus.WORKED.getValue(), result.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getEservice());
        verify(inadService, times(1)).callEService(any(), any(), any());
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
        verifyNoInteractions(ipaService);
    }

    @Test
    void testStatoImpresaNullWithNullPecListFallbackToInad() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(null);
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDto);
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(validInadResponse()));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(GatewayDownstreamService.INAD.name(), result.getEservice());
        verify(inadService, times(1)).callEService(any(), any(), any());
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
    }

    @Test
    void testStatoImpresaNullWithOnlyInvalidPecFallbackToInad() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf("invalid_pec", "not-an-email"));
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(validInadResponse()));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(BatchStatus.WORKED.getValue(), result.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getEservice());
        verify(inadService, times(1)).callEService(any(), any(), any());
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
        verifyNoInteractions(ipaService);
    }

    @Test
    void testStatoImpresaNullWithOnlyInvalidPecInadCfNotFound() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf("invalid_pec"));
        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.error(new PnNationalRegistriesException("CF non trovato", 404, "Not Found",
                        null, null, Charset.defaultCharset(), null)));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(BatchStatus.WORKED.getValue(), result.getStatus());
        verify(inadService, times(1)).callEService(any(), any(), any());
    }

    @Test
    void testStatoImpresaNullWithOnlyInvalidPecInadTechnicalError() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf("invalid_pec"));
        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.error(new PnNationalRegistriesException("Internal Server Error", 500, "Internal Server Error",
                        null, null, Charset.defaultCharset(), null)));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(BatchStatus.ERROR.getValue(), result.getStatus());
        verify(inadService, times(1)).callEService(any(), any(), any());
    }

    @Test
    void testStatoImpresaNullWithValidPec() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf("azienda1@pec.it", "azienda2@pec.it"));
        when(infoCamereConverter.updateBatchRequestFields(any(BatchRequest.class), any(BatchStatus.class), any(LocalDateTime.class), any(CodeSqsDto.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        BatchRequest result = digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        ArgumentCaptor<CodeSqsDto> captor = ArgumentCaptor.forClass(CodeSqsDto.class);
        verify(infoCamereConverter, times(1))
                .updateBatchRequestFields(same(batchRequest), eq(BatchStatus.WORKED), any(LocalDateTime.class), captor.capture());
        assertEquals(List.of("azienda1@pec.it", "azienda2@pec.it"),
                captor.getValue().getDigitalAddress().stream().map(DigitalAddress::getAddress).toList());
        verifyNoInteractions(inadService, ipaService, iniPecBatchRequestService);
    }

    @Test
    void testStatoImpresaNullWithMixedPecPublishesOnlyValid() {
        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        Pec pec = new Pec();

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), any(Pec.class)))
                .thenReturn(codeSqsDtoOf("invalid_pec", "valid@pec.it", "not-an-email"));
        when(infoCamereConverter.updateBatchRequestFields(any(BatchRequest.class), any(BatchStatus.class), any(LocalDateTime.class), any(CodeSqsDto.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        digitalAddressBatchPollingService
                .evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        ArgumentCaptor<CodeSqsDto> captor = ArgumentCaptor.forClass(CodeSqsDto.class);
        verify(infoCamereConverter, times(1))
                .updateBatchRequestFields(same(batchRequest), eq(BatchStatus.WORKED), any(LocalDateTime.class), captor.capture());
        assertEquals(List.of("valid@pec.it"),
                captor.getValue().getDigitalAddress().stream().map(DigitalAddress::getAddress).toList());
        verifyNoInteractions(inadService, ipaService);
    }

    @Test
    void testBatchPecPollingStatoImpresaNullOnlyInvalidPecFallbackToInadAndSendToSqs() {
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId("batchId");
        batchPolling.setPollingId("pollingId");
        batchPolling.setCreatedAt(LocalDateTime.now());

        BatchRequest batchRequest = newStatoImpresaNullBatchRequest();
        batchRequest.setBatchId("batchId");
        batchRequest.setCf("12345678901");

        Pec pec = new Pec();
        pec.setCf("12345678901");
        pec.setStatoImpresa(null);
        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setElencoPec(List.of(pec));

        when(batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(batchPolling))));
        when(batchPollingRepository.setNewReservationIdToBatchPolling(same(batchPolling))).thenReturn(Mono.just(batchPolling));
        when(batchPollingRepository.update(same(batchPolling))).thenReturn(Mono.just(batchPolling));
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.just(iniPecPollingResponse));
        when(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(iniPecBatchSqsService.batchSendToSqs(anyList())).thenReturn(Mono.empty());
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(any(String.class), same(pec)))
                .thenReturn(codeSqsDtoOf("invalid_pec"));
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(validInadResponse()));

        assertDoesNotThrow(() -> digitalAddressBatchPollingService.batchPecPolling());

        verify(inadService, times(1)).callEService(any(), any(), any());
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
        verify(iniPecBatchSqsService, times(1)).batchSendToSqs(List.of(batchRequest));
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batchRequest.getEservice());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
    }

    private static BatchRequest newStatoImpresaNullBatchRequest() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("testBatchId");
        batchRequest.setCorrelationId("testCorrelationId");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());
        batchRequest.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        return batchRequest;
    }

    private static CodeSqsDto codeSqsDtoOf(String... addresses) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        List<DigitalAddress> digitalAddresses = new ArrayList<>();
        for (String address : addresses) {
            DigitalAddress digitalAddress = new DigitalAddress();
            digitalAddress.setAddress(address);
            digitalAddresses.add(digitalAddress);
        }
        codeSqsDto.setDigitalAddress(digitalAddresses);
        return codeSqsDto;
    }

    private static GetDigitalAddressINADOKDto validInadResponse() {
        GetDigitalAddressINADOKDto inadResp = new GetDigitalAddressINADOKDto();
        inadResp.setTaxId("28223913097");
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("inad@pec.it");
        inadResp.setDigitalAddress(digitalAddressDto);
        return inadResp;
    }

    @Test
    void handlePecNotFoundResponse_shouldInitializeInadBatchRequestWhenBatchModeIsEnabled() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setReferenceRequestDate(LocalDateTime.now().minusDays(1));
        batchRequest.setRecipientType(PG.name());
        batchRequest.setCf("12345678901");
        batchRequest.setClientId("clientId");

        when(nationalRegistriesConfig.isEnableInadMassiveApi()).thenReturn(true);
        when(nationalRegistriesRequestsRepository.putRequest(any())).thenReturn(Mono.just(new NationalRegistriesRequest()));

        BatchRequest result = digitalAddressBatchPollingService
                .handlePecNotFoundResponse(batchRequest, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        verifyNoInteractions(inadService);
    }

    @Test
    void handlePecNotFoundResponse_shouldPublishEmptyResultForPersistedPfWithoutCallingInad() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setRecipientType(PF.name());
        batchRequest.setCf("RSSMRA80A01H501U");

        CodeSqsDto emptyInipecResult = new CodeSqsDto();
        emptyInipecResult.setRegistry("INIPEC");
        emptyInipecResult.setDigitalAddress(Collections.emptyList());

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest.getCorrelationId(), null)).thenReturn(emptyInipecResult);
        when(infoCamereConverter.updateBatchRequestFields(eq(batchRequest), eq(BatchStatus.WORKED), any(LocalDateTime.class), same(emptyInipecResult)))
                .thenReturn(Mono.just(batchRequest));

        BatchRequest result = digitalAddressBatchPollingService
                .handlePecNotFoundResponse(batchRequest, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        verify(infoCamereConverter).convertResponsePecToCodeSqsDto(batchRequest.getCorrelationId(), null);
        verify(infoCamereConverter).updateBatchRequestFields(eq(batchRequest), eq(BatchStatus.WORKED), any(LocalDateTime.class), same(emptyInipecResult));
        verifyNoInteractions(inadService);
    }

    @Test
    void handlePecNotFoundResponse_shouldFallbackToInadForPersistedPgWhenBatchModeIsDisabled() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setRecipientType(PG.name());
        batchRequest.setCf("12345678901");

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId("12345678901");
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("pg@pec.it");
        inadResponse.setDigitalAddress(digitalAddressDto);

        when(nationalRegistriesConfig.isEnableInadMassiveApi()).thenReturn(false);
        when(inadService.callEService(any(), eq(PG), eq("correlationId"))).thenReturn(Mono.just(inadResponse));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("serialized-message");

        BatchRequest result = digitalAddressBatchPollingService
                .handlePecNotFoundResponse(batchRequest, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        assertEquals(BatchStatus.WORKED.getValue(), batchRequest.getStatus());
        assertEquals("serialized-message", batchRequest.getMessage());
        assertEquals("INAD", batchRequest.getEservice());
        verify(inadService).callEService(any(), eq(PG), eq("correlationId"));
    }

    @Test
    void handlePecNotFoundResponse_shouldUseLegacyInferenceAndStillCallInadWhenRecipientTypeIsMissing() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setRecipientType(null);
        batchRequest.setCf("RSSMRA80A01H501U");

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId("RSSMRA80A01H501U");
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("pf@pec.it");
        inadResponse.setDigitalAddress(digitalAddressDto);

        when(nationalRegistriesConfig.isEnableInadMassiveApi()).thenReturn(false);
        when(inadService.callEService(any(), eq(PF), eq("correlationId"))).thenReturn(Mono.just(inadResponse));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("serialized-message");

        BatchRequest result = digitalAddressBatchPollingService
                .handlePecNotFoundResponse(batchRequest, BatchStatus.WORKED, LocalDateTime.now())
                .block();

        assertSame(batchRequest, result);
        verify(inadService).callEService(any(), eq(PF), eq("correlationId"));
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
    }

    @Test
    void evaluateStatoImpresa_shouldFallbackToInadForPgWhenAllInipecAddressesAreInvalidAccordingToCurrentCode() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setRecipientType(PG.name());
        batchRequest.setCf("12345678901");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());

        Pec pec = new Pec();
        pec.setCf("12345678901");
        pec.setStatoImpresa(null);

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        DigitalAddress invalidAddress = new DigitalAddress();
        invalidAddress.setAddress("not-an-email");
        codeSqsDto.setDigitalAddress(List.of(invalidAddress));

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId("12345678901");
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("fallback@pec.it");
        inadResponse.setDigitalAddress(digitalAddressDto);

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest.getCorrelationId(), pec)).thenReturn(codeSqsDto);
        when(nationalRegistriesConfig.isEnableInadMassiveApi()).thenReturn(false);
        when(inadService.callEService(any(), eq(PG), eq("correlationId"))).thenReturn(Mono.just(inadResponse));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("serialized-message");

        BatchRequest result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKING, LocalDateTime.now()).block();

        assertSame(batchRequest, result);
        verify(inadService).callEService(any(), eq(PG), eq("correlationId"));
        verify(infoCamereConverter, never()).updateBatchRequestFields(any(), any(), any(), any());
    }

    @Test
    void evaluateStatoImpresa_shouldPublishEmptyResultForPfWhenNdOccurs() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId~1");
        batchRequest.setRecipientType(PF.name());
        batchRequest.setCf("RSSMRA80A01H501U");
        batchRequest.setStatus(BatchStatus.WORKING.getValue());

        Pec pec = new Pec();
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ND);

        CodeSqsDto emptyInipecResult = new CodeSqsDto();
        emptyInipecResult.setRegistry("INIPEC");
        emptyInipecResult.setDigitalAddress(Collections.emptyList());

        when(infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest.getCorrelationId(), null)).thenReturn(emptyInipecResult);
        when(infoCamereConverter.updateBatchRequestFields(eq(batchRequest), eq(BatchStatus.WORKING), any(LocalDateTime.class), same(emptyInipecResult)))
                .thenReturn(Mono.just(batchRequest));

        BatchRequest result = digitalAddressBatchPollingService.evaluateStatoImpresa(batchRequest, pec, BatchStatus.WORKING, LocalDateTime.now()).block();

        assertSame(batchRequest, result);
        verify(infoCamereConverter).updateBatchRequestFields(eq(batchRequest), eq(BatchStatus.WORKING), any(LocalDateTime.class), same(emptyInipecResult));
        verifyNoInteractions(inadService);
    }
}
