package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.InipecEvaluationResult;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InipecPollingServiceTest {

    private static final String BATCH_ID = "batch-id";
    private static final String POLLING_ID = "polling-id";
    private static final String SEPARATOR = "~";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String REQUEST_CORRELATION_ID = CORRELATION_ID + SEPARATOR + "1";
    private static final String TAX_ID_PG = "12345678901";
    private static final int MAX_RETRY = 3;
    private static final int REQUEST_RETRY_AFTER = 60;

    @Mock
    private InfoCamereService infoCamereService;
    @Mock
    private NationalRegistriesRequestsRepository requestsRepository;
    @Mock
    private InfoCamereConverter infoCamereConverter;
    @Mock
    private InadService inadService;
    @Mock
    private GatewayUtils gatewayUtils;
    @Mock
    private PollingService pollingService;
    @Mock
    private Page<NationalRegistriesRequest> page;

    private NationalRegistriesConfig config;
    private InipecPollingService service;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setBatchPollingMaxRetry(5);
        inipec.setBatchPollingInProgressMaxRetry(24);
        inipec.setBatchRequestPkSeparator(SEPARATOR);
        inipec.setBatchRequestMaxRetry(MAX_RETRY);
        inipec.setRequestRetryAfter(REQUEST_RETRY_AFTER);

        config = new NationalRegistriesConfig();
        config.setInipec(inipec);

        service = new InipecPollingService(infoCamereService, requestsRepository, infoCamereConverter, config,
                inadService, gatewayUtils, pollingService);
    }

    // ---- errore nella chiamata di polling ----

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pollingErrorIsHandledByRetryLogic(boolean inProgress) {
        IllegalStateException error = new IllegalStateException("polling failed");
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.error(error));
        when(infoCamereConverter.isPollingResponseNotReady(error)).thenReturn(inProgress);
        when(pollingService.incrementAndCheckRetry(BATCH_ID, 1, 2, inProgress, 5, 24, GatewayDownstreamService.INIPEC, error))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(1, 2))).verifyComplete();

        verify(pollingService).incrementAndCheckRetry(BATCH_ID, 1, 2, inProgress, 5, 24, GatewayDownstreamService.INIPEC, error);
        verify(pollingService, never()).markBatchAsWorked(anyString(), any());
        verifyNoInteractions(requestsRepository);
    }

    @Test
    void retryLogicErrorIsPropagated() {
        IllegalStateException error = new IllegalStateException("polling failed");
        IllegalArgumentException retryError = new IllegalArgumentException("retry exhausted");
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.error(error));
        when(infoCamereConverter.isPollingResponseNotReady(error)).thenReturn(false);
        when(pollingService.incrementAndCheckRetry(anyString(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(Mono.error(retryError));

        StepVerifier.create(service.handleMessage(payload(0, 0)))
                .expectErrorMatches(e -> e == retryError)
                .verify();
    }

    // ---- polling riuscito ----

    @Test
    void successfulPollingWithoutRequestsMarksBatchAsWorked() {
        mockPollingSuccess(List.of());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC);
        verify(pollingService, never()).markRequestAsWorked(any(), any(), any(), anyBoolean());
    }

    @Test
    void successfulPollingWithNullBatchSizeMarksBatchAsWorked() {
        mockPollingSuccess(List.of());
        PnPollingEvent.Payload payload = payload(0, 0);
        payload.setBatchSize(null);

        StepVerifier.create(service.handleMessage(payload)).verifyComplete();

        verify(pollingService).markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC);
    }

    @Test
    void pecFoundIsMarkedAsWorkedWithPayloadAndPublishedResult() {
        NationalRegistriesRequest request = request("PF", "RSSMRA80A01H501U", null);
        Pec pec = new Pec();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, request.getTaxId(), "PF", pec))
                .thenReturn(new InipecEvaluationResult.Found(codeSqsDto));
        when(gatewayUtils.convertCodeSqsDtoToString(codeSqsDto)).thenReturn("payload");
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true);
        verify(pollingService).markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC);
        verifyNoInteractions(inadService);
    }

    // ---- PEC non trovata ----

    @ParameterizedTest
    @ValueSource(strings = {"PF", "pf"})
    void pecMissingForPfIsMarkedAsWorkedWithEmptyResult(String recipientType) {
        NationalRegistriesRequest request = request(recipientType, "RSSMRA80A01H501U", null);
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(REQUEST_CORRELATION_ID, null)).thenReturn(codeSqsDto);
        when(gatewayUtils.convertCodeSqsDtoToString(codeSqsDto)).thenReturn("payload");
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true);
        verifyNoInteractions(inadService);
        verify(pollingService, never()).createFallbackRequest(any(), any(), any());
    }

    @Test
    void notFoundEvaluationForPfIsMarkedAsWorkedWithEmptyResult() {
        NationalRegistriesRequest request = request("PF", "RSSMRA80A01H501U", null);
        Pec pec = new Pec();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, request.getTaxId(), "PF", pec))
                .thenReturn(new InipecEvaluationResult.NotFound());
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(REQUEST_CORRELATION_ID, null)).thenReturn(codeSqsDto);
        when(gatewayUtils.convertCodeSqsDtoToString(codeSqsDto)).thenReturn("payload");
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).markRequestAsWorked(request, GatewayDownstreamService.INIPEC, "payload", true);
    }

    // ---- flag enable-inad-massive-api ----

    @Test
    void pecMissingForPgWithInadMassiveApiEnabledCreatesFallbackRequest() {
        config.setEnableInadMassiveApi(true);
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
        verifyNoInteractions(inadService);
        verify(requestsRepository, never()).putRequest(any());
    }

    @Test
    void notFoundEvaluationForPgWithInadMassiveApiEnabledCreatesFallbackRequest() {
        config.setEnableInadMassiveApi(true);
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Pec pec = new Pec();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, TAX_ID_PG, "PG", pec))
                .thenReturn(new InipecEvaluationResult.NotFound());
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
        verifyNoInteractions(inadService);
    }

    @Test
    void pecMissingForPgWithInadMassiveApiDisabledCallsInadSynchronously() {
        config.setEnableInadMassiveApi(false);
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);

        GetDigitalAddressINADOKDto inadResponse = inadResponse(TAX_ID_PG, "pec@pec.it");
        when(inadService.callEService(any(), eq(it.pagopa.pn.national.registries.constant.RecipientType.PG), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("inad-payload");
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<NationalRegistriesRequest> captor = ArgumentCaptor.forClass(NationalRegistriesRequest.class);
        verify(requestsRepository).putRequest(captor.capture());
        NationalRegistriesRequest saved = captor.getValue();
        assertEquals(GatewayDownstreamService.INAD, saved.getRegistry());
        assertEquals(RequestStatusEnum.WORKED, saved.getStatus());
        assertEquals("inad-payload", saved.getResultPayload());
        assertEquals(REQUEST_CORRELATION_ID, saved.getCorrelationId());
        verify(pollingService).markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false);
        verify(pollingService, never()).createFallbackRequest(any(), any(), any());
    }

    @Test
    void inadSyncResponseWithoutDigitalAddressIsSavedAsWorked() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId(TAX_ID_PG);
        when(inadService.callEService(any(), any(), eq(CORRELATION_ID))).thenReturn(Mono.just(inadResponse));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("inad-payload");
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<CodeSqsDto> sqsCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);
        verify(gatewayUtils).convertCodeSqsDtoToString(sqsCaptor.capture());
        assertTrue(sqsCaptor.getValue().getDigitalAddress().isEmpty());
        assertEquals("INAD", sqsCaptor.getValue().getRegistry());
    }

    @Test
    void inadSyncInvalidEmailIsTreatedAsNotFound() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(inadService.callEService(any(), any(), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse(TAX_ID_PG, "not-an-email")));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("inad-payload");
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<NationalRegistriesRequest> captor = ArgumentCaptor.forClass(NationalRegistriesRequest.class);
        verify(requestsRepository).putRequest(captor.capture());
        assertEquals(RequestStatusEnum.WORKED, captor.getValue().getStatus());
        assertEquals(GatewayDownstreamService.INAD, captor.getValue().getRegistry());
        assertEquals("inad-payload", captor.getValue().getResultPayload());
    }

    @Test
    void inadSyncNotFoundErrorIsSavedAsWorkedWithEmptyAddress() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(inadService.callEService(any(), any(), eq(CORRELATION_ID))).thenReturn(Mono.error(inadException(404, "CF non trovato")));
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("inad-payload");
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<NationalRegistriesRequest> captor = ArgumentCaptor.forClass(NationalRegistriesRequest.class);
        verify(requestsRepository).putRequest(captor.capture());
        assertEquals(RequestStatusEnum.WORKED, captor.getValue().getStatus());
        assertEquals("inad-payload", captor.getValue().getResultPayload());
        verify(gatewayUtils).logEServiceError(any(Throwable.class), anyString());
    }

    @Test
    void inadSyncGenericErrorIsSavedAsErrorWithoutPayload() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(inadService.callEService(any(), any(), eq(CORRELATION_ID))).thenReturn(Mono.error(inadException(500, "boom")));
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<NationalRegistriesRequest> captor = ArgumentCaptor.forClass(NationalRegistriesRequest.class);
        verify(requestsRepository).putRequest(captor.capture());
        assertEquals(RequestStatusEnum.ERROR, captor.getValue().getStatus());
        assertEquals(GatewayDownstreamService.INAD, captor.getValue().getRegistry());
        assertNull(captor.getValue().getResultPayload());
        verify(gatewayUtils, never()).convertCodeSqsDtoToString(any());
    }

    @Test
    void inadSyncUsesRecipientTypeInferredFromTaxIdWhenMissing() {
        NationalRegistriesRequest request = request(null, TAX_ID_PG, null);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(null);
        when(inadService.callEService(any(), eq(it.pagopa.pn.national.registries.constant.RecipientType.PG), eq(CORRELATION_ID)))
                .thenReturn(Mono.error(inadException(500, "boom")));
        when(requestsRepository.putRequest(any(NationalRegistriesRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(inadService).callEService(any(), eq(it.pagopa.pn.national.registries.constant.RecipientType.PG), eq(CORRELATION_ID));
    }

    // ---- stato impresa "da riprovare" ----

    @Test
    void retryEvaluationBelowMaxResetsRequestForRetry() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, 1);
        Pec pec = new Pec();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, TAX_ID_PG, "PG", pec))
                .thenReturn(new InipecEvaluationResult.Retry());
        when(requestsRepository.resetStatusForRetry(eq(REQUEST_CORRELATION_ID), eq(request.getCreatedAt().toString()), eq(2), any(Instant.class), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(request));

        Instant before = Instant.now();
        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        ArgumentCaptor<Instant> attemptAfter = ArgumentCaptor.forClass(Instant.class);
        verify(requestsRepository).resetStatusForRetry(eq(REQUEST_CORRELATION_ID), eq(request.getCreatedAt().toString()), eq(2), attemptAfter.capture(), eq(GatewayDownstreamService.INIPEC));
        assertFalse(attemptAfter.getValue().isBefore(before.plusSeconds(REQUEST_RETRY_AFTER)));
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void retryEvaluationWithoutPreviousRetriesStartsFromOne() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        Pec pec = new Pec();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, TAX_ID_PG, "PG", pec))
                .thenReturn(new InipecEvaluationResult.Retry());
        when(requestsRepository.resetStatusForRetry(anyString(), anyString(), eq(1), any(Instant.class), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(request));

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(requestsRepository).resetStatusForRetry(anyString(), anyString(), eq(1), any(Instant.class), eq(GatewayDownstreamService.INIPEC));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 10})
    void retryEvaluationAtOrAboveMaxMarksRequestAsError(int previousRetries) {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, previousRetries);
        Pec pec = new Pec();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(REQUEST_CORRELATION_ID, TAX_ID_PG, "PG", pec))
                .thenReturn(new InipecEvaluationResult.Retry());
        when(requestsRepository.updateStatusToError(REQUEST_CORRELATION_ID, request.getCreatedAt().toString(), GatewayDownstreamService.INIPEC))
                .thenReturn(Mono.just(request));

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(requestsRepository).updateStatusToError(REQUEST_CORRELATION_ID, request.getCreatedAt().toString(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository, never()).resetStatusForRetry(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void errorResettingRequestForRetryIsPropagated() {
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, 0);
        Pec pec = new Pec();
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(request));
        when(infoCamereConverter.retrievePec(request.getTaxId(), pecByTaxId)).thenReturn(pec);
        when(infoCamereService.evaluateInipecStatoImpresa(anyString(), anyString(), anyString(), any()))
                .thenReturn(new InipecEvaluationResult.Retry());
        when(requestsRepository.resetStatusForRetry(anyString(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("dynamo down")));

        StepVerifier.create(service.handleMessage(payload(0, 0)))
                .expectError(IllegalStateException.class)
                .verify();
    }

    /**
     * Regression: al replay di un batch con tutte le request già WORKED il repository non restituisce elementi
     * WORKING. Non deve essere ricreato alcun fallback INAD e deve essere completata solo la chiusura del batch.
     */
    @Test
    void successfulPollingReplayWithoutWorkingRequestsOnlyClosesBatch() {
        IniPecPollingResponse response = new IniPecPollingResponse();
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.just(response));
        when(infoCamereConverter.mapPecByTaxId(response)).thenReturn(Map.of());
        when(requestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.empty()));
        AtomicBoolean batchCloseSubscribed = new AtomicBoolean(false);
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC)).thenReturn(Mono.defer(() -> {
            batchCloseSubscribed.set(true);
            return Mono.empty();
        }));

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        assertTrue(batchCloseSubscribed.get());
        verify(requestsRepository).getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING);
        verify(pollingService, never()).createFallbackRequest(any(), any(), any());
        verify(pollingService, never()).markRequestAsWorked(any(), any(), any(), anyBoolean());
    }

    /**
     * Regression: se la creazione del fallback INAD fallisce durante il processing della request INIPEC,
     * il Mono di chiusura batch non deve essere sottoscritto e il batch deve restare recuperabile in WORKING.
     */
    @Test
    void successfulPollingDoesNotCloseBatchWhenFallbackCreationFails() {
        config.setEnableInadMassiveApi(true);
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        IniPecPollingResponse response = new IniPecPollingResponse();
        Map<String, Pec> pecByTaxId = Map.of();
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.just(response));
        when(infoCamereConverter.mapPecByTaxId(response)).thenReturn(pecByTaxId);
        when(infoCamereConverter.retrievePec(TAX_ID_PG, pecByTaxId)).thenReturn(null);
        when(page.items()).thenReturn(List.of(request));
        when(requestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD)).thenReturn(Mono.error(new IllegalStateException("request update failed")));
        AtomicBoolean batchCloseSubscribed = new AtomicBoolean(false);
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC)).thenReturn(Mono.defer(() -> {
            batchCloseSubscribed.set(true);
            return Mono.empty();
        }));

        StepVerifier.create(service.handleMessage(payload(0, 0))).expectError(IllegalStateException.class).verify();

        assertFalse(batchCloseSubscribed.get());
        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
    }

    /**
     * Regression: se il fallback è stato creato ma fallisce solo la chiusura finale del batch, l'errore deve
     * essere propagato dopo il processing della request, lasciando al replay il solo compito di chiudere il batch.
     */
    @Test
    void successfulPollingPropagatesBatchCloseFailureAfterFallbackCreation() {
        config.setEnableInadMassiveApi(true);
        NationalRegistriesRequest request = request("PG", TAX_ID_PG, null);
        IniPecPollingResponse response = new IniPecPollingResponse();
        Map<String, Pec> pecByTaxId = Map.of();
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.just(response));
        when(infoCamereConverter.mapPecByTaxId(response)).thenReturn(pecByTaxId);
        when(infoCamereConverter.retrievePec(TAX_ID_PG, pecByTaxId)).thenReturn(null);
        when(page.items()).thenReturn(List.of(request));
        when(requestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());
        IllegalStateException batchError = new IllegalStateException("batch update failed");
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC)).thenReturn(Mono.error(batchError));

        StepVerifier.create(service.handleMessage(payload(0, 0))).expectErrorMatches(error -> error == batchError).verify();

        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
    }

    // ---- richieste multiple ----

    @Test
    void everyRequestOfTheBatchIsProcessed() {
        NationalRegistriesRequest pf = request("PF", "RSSMRA80A01H501U", null);
        pf.setCorrelationId("pf-corr" + SEPARATOR + "1");
        NationalRegistriesRequest pg = request("PG", TAX_ID_PG, null);
        pg.setCorrelationId("pg-corr" + SEPARATOR + "2");
        config.setEnableInadMassiveApi(true);
        Map<String, Pec> pecByTaxId = mockPollingSuccess(List.of(pf, pg));
        when(infoCamereConverter.retrievePec(anyString(), eq(pecByTaxId))).thenReturn(null);
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(infoCamereConverter.convertResponsePecToCodeSqsDto(pf.getCorrelationId(), null)).thenReturn(codeSqsDto);
        when(gatewayUtils.convertCodeSqsDtoToString(codeSqsDto)).thenReturn("payload");
        when(pollingService.markRequestAsWorked(pf, GatewayDownstreamService.INIPEC, "payload", true)).thenReturn(Mono.empty());
        when(pollingService.createFallbackRequest(pg, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(service.handleMessage(payload(0, 0))).verifyComplete();

        verify(pollingService).markRequestAsWorked(pf, GatewayDownstreamService.INIPEC, "payload", true);
        verify(pollingService).createFallbackRequest(pg, GatewayDownstreamService.INIPEC, GatewayDownstreamService.INAD);
        verify(pollingService).markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC);
    }

    // ---- helper ----

    private Map<String, Pec> mockPollingSuccess(List<NationalRegistriesRequest> requests) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        Map<String, Pec> pecByTaxId = Map.of();
        when(infoCamereService.callIniPecEService(BATCH_ID, POLLING_ID)).thenReturn(Mono.just(response));
        when(infoCamereConverter.mapPecByTaxId(response)).thenReturn(pecByTaxId);
        when(page.items()).thenReturn(requests);
        when(requestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());
        return pecByTaxId;
    }

    private static PnPollingEvent.Payload payload(int errorRetryCount, int inProgressRetryCount) {
        return PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(BATCH_ID)
                .pollingId(POLLING_ID)
                .batchSize(1)
                .errorRetryCount(errorRetryCount)
                .inProgressRetryCount(inProgressRetryCount)
                .build();
    }

    private static NationalRegistriesRequest request(String recipientType, String taxId, Integer retryCount) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(REQUEST_CORRELATION_ID);
        request.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        request.setTaxId(taxId);
        request.setRecipientType(recipientType);
        request.setRetryCount(retryCount);
        return request;
    }

    private static GetDigitalAddressINADOKDto inadResponse(String taxId, String address) {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress(address);
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(taxId);
        response.setDigitalAddress(digitalAddress);
        return response;
    }

    private static PnNationalRegistriesException inadException(int status, String message) {
        return new PnNationalRegistriesException(message, status, "status", new HttpHeaders(),
                new byte[0], StandardCharsets.UTF_8, Object.class);
    }
}

