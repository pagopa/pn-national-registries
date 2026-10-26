package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InadPollingServiceTest {

    private static final String BATCH_ID = "batch-id";
    private static final String POLLING_ID = "polling-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String TAX_ID = "RSSMRA80A01H501U";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private InadClient inadClient;

    @Mock
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @Mock
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Mock
    private NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    private NationalRegistriesConfig.Inad inadConfig;

    @Mock
    private InadConverter inadConverter;

    @Mock
    private GatewayUtils gatewayUtils;

    @Mock
    private PollingService pollingService;

    private InadPollingService inadPollingService;

    @BeforeEach
    void setUp() {
        inadPollingService = new InadPollingService(inadClient, nationalRegistriesRequestsRepository, nationalRegistriesConfig, inadConverter, gatewayUtils, pollingService);
    }

    @Test
    void handleMessage_shouldIncrementInProgressRetry_whenStatusIsPresaInCarico() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(StatusProcessingRequest.PRESA_IN_CARICO);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(response));
        mockRetryConfiguration();
        when(pollingService.incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), true, 5, 10, GatewayDownstreamService.INAD, null)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(pollingService).incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), true, 5, 10, GatewayDownstreamService.INAD, null);
        verify(inadClient, never()).getMassiveRequestResult(anyString());
    }

    @Test
    void handleMessage_shouldIncrementInProgressRetry_whenStatusIsInElaborazione() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(StatusProcessingRequest.IN_ELABORAZIONE);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(response));
        mockRetryConfiguration();
        when(pollingService.incrementAndCheckRetry(anyString(), any(), any(), eq(true), anyInt(), anyInt(), eq(GatewayDownstreamService.INAD), isNull())).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(pollingService).incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), true, 5, 10, GatewayDownstreamService.INAD, null);
    }

    @Test
    void handleMessage_shouldRetrieveResult_whenStatusIsDisponibile() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));
        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.empty()));
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(inadClient).getMassiveRequestResult(POLLING_ID);
        verify(pollingService).markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD);
    }

    @Test
    void handleMessage_shouldFail_whenPollingResponseIsNull() {
        PnPollingEvent.Payload payload = buildPayload();
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verifyNoInteractions(pollingService);
    }

    @Test
    void handleMessage_shouldPropagateInternalException_whenStateIsNull() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(null);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(response));

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(pollingService);
    }

    @Test
    void handleMessage_shouldMarkNotFoundRequestAsWorked_whenTaxIdMissingAndFallbackDisabled() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));
        NationalRegistriesRequest request = buildRequest();
        Page<NationalRegistriesRequest> page = Page.create(List.of(request));
        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class))).thenReturn("{\"result\":\"not-found\"}");
        when(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INAD, "{\"result\":\"not-found\"}", true)).thenReturn(Mono.empty());
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).markRequestAsWorked(request, GatewayDownstreamService.INAD, "{\"result\":\"not-found\"}", true);
    }

    @Test
    void handleMessage_shouldCreateInipecFallback_whenPfAndAddressNotFound() {
        PnPollingEvent.Payload payload = buildPayload();
        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));

        NationalRegistriesRequest request = buildRequest();
        request.setRecipientType(RecipientType.PF.name());

        Page<NationalRegistriesRequest> page = Page.create(List.of(request));

        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC);
        verify(pollingService, never()).markRequestAsWorked(eq(request), eq(GatewayDownstreamService.INAD), any(), eq(true));
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenStatusCallReturnsPnNationalRegistriesException() {
        PnPollingEvent.Payload payload = buildPayload();
        PnNationalRegistriesException exception = mockPnNationalRegistriesException();
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.error(exception));
        mockRetryConfiguration();
        when(pollingService.incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, 5, 10, GatewayDownstreamService.INAD, exception)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(pollingService).incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, 5, 10, GatewayDownstreamService.INAD, exception);
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenResultCallReturnsPnNationalRegistriesException() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        RuntimeException exception = mockPnNationalRegistriesException();
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.error(exception));
        mockRetryConfiguration();
        when(pollingService.incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, 5, 10, GatewayDownstreamService.INAD, exception)).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload)).verifyComplete();

        verify(pollingService).incrementAndCheckRetry(BATCH_ID, payload.getErrorRetryCount(), payload.getInProgressRetryCount(), false, 5, 10, GatewayDownstreamService.INAD, exception);
    }

    private void mockRetryConfiguration() {
        when(nationalRegistriesConfig.getInad()).thenReturn(inadConfig);
        when(inadConfig.getBatchPollingMaxRetry()).thenReturn(5);
        when(inadConfig.getBatchPollingInProgressMaxRetry()).thenReturn(10);
    }

    private PnPollingEvent.Payload buildPayload() {
        PnPollingEvent.Payload payload = new PnPollingEvent.Payload();
        payload.setBatchId(BATCH_ID);
        payload.setPollingId(POLLING_ID);
        payload.setBatchSize(1);
        payload.setErrorRetryCount(1);
        payload.setInProgressRetryCount(2);
        return payload;
    }

    private NationalRegistriesRequest buildRequest() {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(CORRELATION_ID);
        request.setTaxId(TAX_ID);
        request.setRecipientType(RecipientType.PF.name());
        request.setRegistry(GatewayDownstreamService.INAD);
        request.setStatus(RequestStatusEnum.WORKING);
        request.setCreatedAt(CREATED_AT);
        return request;
    }

    private PnNationalRegistriesException mockPnNationalRegistriesException() {
        return mock(PnNationalRegistriesException.class);
    }
}