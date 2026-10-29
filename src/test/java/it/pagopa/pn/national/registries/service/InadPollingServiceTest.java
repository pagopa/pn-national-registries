package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    private InadConverter inadConverter;

    @Mock
    private GatewayUtils gatewayUtils;

    @Mock
    private PollingService pollingService;

    private InadPollingService inadPollingService;

    @BeforeEach
    void setUp() {
        inadPollingService = new InadPollingService(
                inadClient,
                nationalRegistriesRequestsRepository,
                nationalRegistriesConfig,
                inadConverter,
                gatewayUtils,
                pollingService
        );
    }

    @Test
    void handleMessage_shouldIncrementInProgressRetry_whenStatusIsPresaInCarico() {
        PnPollingEvent.Payload payload = buildPayload();
        mockPollingConfig();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(StatusProcessingRequest.PRESA_IN_CARICO);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(response));

        when(pollingService.incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                true,
                5,
                10,
                GatewayDownstreamService.INAD,
                null
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                true,
                5,
                10,
                GatewayDownstreamService.INAD,
                null
        );

        verify(inadClient, never()).getMassiveRequestResult(anyString());
    }

    @Test
    void handleMessage_shouldIncrementInProgressRetry_whenStatusIsInElaborazione() {
        PnPollingEvent.Payload payload = buildPayload();
        mockPollingConfig();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(StatusProcessingRequest.IN_ELABORAZIONE);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(response));

        when(pollingService.incrementAndCheckRetry(
                anyString(),
                any(),
                any(),
                eq(true),
                anyInt(),
                anyInt(),
                eq(GatewayDownstreamService.INAD),
                isNull()
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                true,
                5,
                10,
                GatewayDownstreamService.INAD,
                null
        );
    }

    @Test
    void handleMessage_shouldRetrieveResult_whenStatusIsDisponibile() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);

        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.just(resultResponse));
        when(inadConverter.mapResponseByTaxId(resultResponse))
                .thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.empty()));
        when(pollingService.markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(inadClient).getMassiveRequestResult(POLLING_ID);
        verify(pollingService).markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        );
    }

    @Test
    void handleMessage_shouldPropagateInternalException_whenStateIsNull() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress response = mock(ResponseStatusListDigitalAddress.class);
        when(response.getState()).thenReturn(null);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(response));

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

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.just(resultResponse));

        NationalRegistriesRequest request = buildRequest();
        Page<NationalRegistriesRequest> page = Page.create(List.of(request));

        when(inadConverter.mapResponseByTaxId(resultResponse))
                .thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("{\"result\":\"not-found\"}");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "{\"result\":\"not-found\"}",
                true
        )).thenReturn(Mono.empty());
        when(pollingService.markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "{\"result\":\"not-found\"}",
                true
        );

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldCreateInipecFallback_whenPfAndTaxIdMissingFromResponse() {
        PnPollingEvent.Payload payload = buildPayload();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);

        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.just(resultResponse));

        NationalRegistriesRequest request = buildRequest();
        request.setRecipientType(RecipientType.PF.name());

        Page<NationalRegistriesRequest> page = Page.create(List.of(request));

        when(inadConverter.mapResponseByTaxId(resultResponse))
                .thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);
        when(pollingService.createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());
        when(pollingService.markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        );

        verify(pollingService, never()).markRequestAsWorked(
                eq(request),
                eq(GatewayDownstreamService.INAD),
                any(),
                eq(true)
        );
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenStatusCallReturnsPnNationalRegistriesException() {
        PnPollingEvent.Payload payload = buildPayload();
        PnNationalRegistriesException exception = mockPnNationalRegistriesException();

        mockPollingConfig();

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.error(exception));

        when(pollingService.incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        );
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenResultCallReturnsPnNationalRegistriesException() {
        PnPollingEvent.Payload payload = buildPayload();
        mockPollingConfig();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));

        RuntimeException exception = mockPnNationalRegistriesException();

        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.error(exception));

        when(pollingService.incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        );
    }

    @Test
    void handleMessage_shouldCompleteWithoutRetry_whenPollingResponseIsEmpty() {
        PnPollingEvent.Payload payload = buildPayload();

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verifyNoInteractions(pollingService);
        verify(inadClient, never()).getMassiveRequestResult(anyString());
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenStatusCallFailsWithGenericException() {
        PnPollingEvent.Payload payload = buildPayload();
        RuntimeException exception = new RuntimeException("generic");

        mockPollingConfig();

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.error(exception));

        when(pollingService.incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        );

        verify(inadClient, never()).getMassiveRequestResult(anyString());
    }

    @Test
    void handleMessage_shouldIncrementRetry_whenResultCallFailsWithGenericException() {
        PnPollingEvent.Payload payload = buildPayload();
        RuntimeException exception = new RuntimeException("generic");

        mockPollingConfig();

        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.error(exception));

        when(pollingService.incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(payload))
                .verifyComplete();

        verify(pollingService).incrementAndCheckRetry(
                BATCH_ID,
                payload.getErrorRetryCount(),
                payload.getInProgressRetryCount(),
                false,
                5,
                10,
                GatewayDownstreamService.INAD,
                exception
        );
    }

    @Test
    void handleMessage_shouldMarkRequestAsWorkedWithAddress_whenPfPersonalAddressFoundAndFallbackDisabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(TAX_ID, address("personal@pec.it", null))
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                GatewayDownstreamService.INAD.name().equals(dto.getRegistry())
                        && CORRELATION_ID.equals(dto.getCorrelationId())
                        && dto.getDigitalAddress() != null
                        && dto.getDigitalAddress().size() == 1
                        && "personal@pec.it".equals(
                        dto.getDigitalAddress().getFirst().getAddress()
                )
        ));

        verify(pollingService).markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        );

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldMarkRequestAsWorkedWithPersonalAddress_whenPfPersonalAddressFoundAndFallbackEnabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(
                        TAX_ID,
                        address("personal@pec.it", null),
                        address("pro@pec.it", "AVVOCATO")
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                dto.getDigitalAddress().size() == 1
                        && "personal@pec.it".equals(
                        dto.getDigitalAddress().getFirst().getAddress()
                )
        ));

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldUseProfessionalAddress_whenPfHasOnlyProfessionalAddressAndFallbackEnabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(
                        TAX_ID,
                        address("pro@pec.it", "AVVOCATO")
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                dto.getDigitalAddress().size() == 1
                        && "pro@pec.it".equals(
                        dto.getDigitalAddress().getFirst().getAddress()
                )
        ));

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldMarkRequestAsWorkedWithEmptyAddresses_whenPfHasOnlyProfessionalAddressAndFallbackDisabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(
                        TAX_ID,
                        address("pro@pec.it", "AVVOCATO")
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                dto.getDigitalAddress() != null
                        && dto.getDigitalAddress().isEmpty()
        ));

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldCreateInipecFallback_whenPfHasNoAddressInResponseAndFallbackEnabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(TAX_ID)
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);

        when(pollingService.createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(pollingService).createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        );

        verify(pollingService, never())
                .markRequestAsWorked(any(), any(), any(), anyBoolean());

        verify(pollingService).markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        );
    }

    @Test
    void handleMessage_shouldMarkRequestAsWorkedWithEmptyAddresses_whenPfHasNoAddressInResponseAndFallbackDisabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(TAX_ID)
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());

        verify(pollingService).markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        );
    }

    @Test
    void handleMessage_shouldTreatInvalidPersonalEmailAsNotFound_whenFallbackDisabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(
                        TAX_ID,
                        address("not-an-email", null)
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                dto.getDigitalAddress() != null
                        && dto.getDigitalAddress().isEmpty()
        ));

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());
    }

    @Test
    void handleMessage_shouldCreateInipecFallback_whenPersonalEmailIsInvalidAndFallbackEnabled() {
        NationalRegistriesRequest request = buildRequest();

        prepareAvailableResult(
                request,
                buildInadResponse(
                        TAX_ID,
                        address("not-an-email", null)
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);

        when(pollingService.createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(pollingService).createFallbackRequest(
                request,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        );

        verify(pollingService, never())
                .markRequestAsWorked(any(), any(), any(), anyBoolean());
    }

    @Test
    void handleMessage_shouldMarkPgRequestAsWorkedWithAddress_whenSingleNonProfessionalAddressFound() {
        String pIva = "12345678901";

        NationalRegistriesRequest request = buildRequest();
        request.setTaxId(pIva);
        request.setRecipientType(RecipientType.PG.name());

        prepareAvailableResult(
                request,
                buildInadResponse(
                        pIva,
                        address("company@pec.it", null)
                )
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(false);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(gatewayUtils).convertCodeSqsDtoToString(argThat(dto ->
                dto.getDigitalAddress().size() == 1
                        && "company@pec.it".equals(
                        dto.getDigitalAddress().getFirst().getAddress()
                )
        ));
    }

    @Test
    void handleMessage_shouldNeverCreateInipecFallbackForPg_evenWhenFallbackFlagIsEnabled() {
        String pIva = "12345678901";

        NationalRegistriesRequest request = buildRequest();
        request.setTaxId(pIva);
        request.setRecipientType(RecipientType.PG.name());

        prepareAvailableResult(
                request,
                buildInadResponse(pIva)
        );

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);
        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");
        when(pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(pollingService, never())
                .createFallbackRequest(any(), any(), any());

        verify(pollingService).markRequestAsWorked(
                request,
                GatewayDownstreamService.INAD,
                "payload",
                true
        );
    }

    /**
     * Regression: se tutte le request del batch sono già WORKED al replay, il repository non restituisce
     * elementi WORKING. Il polling non deve ricreare fallback o risultati e deve solo chiudere il batch.
     */
    @Test
    void handleMessage_shouldOnlyCloseBatch_whenReplayFindsNoWorkingRequests() {
        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));
        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.empty()));
        AtomicBoolean batchCloseSubscribed = new AtomicBoolean(false);
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.defer(() -> {
            batchCloseSubscribed.set(true);
            return Mono.empty();
        }));

        StepVerifier.create(inadPollingService.handleMessage(buildPayload())).verifyComplete();

        assertTrue(batchCloseSubscribed.get());
        verify(nationalRegistriesRequestsRepository).getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING);
        verify(pollingService, never()).createFallbackRequest(any(), any(), any());
        verify(pollingService, never()).markRequestAsWorked(any(), any(), any(), anyBoolean());
    }

    /**
     * Regression: se il processing di una request fallisce, il Mono di chiusura batch non deve essere sottoscritto.
     * Il batch resta quindi WORKING e la request può essere recuperata al replay successivo.
     */
    @Test
    void handleMessage_shouldNotCloseBatch_whenRequestProcessingFails() {
        NationalRegistriesRequest request = buildRequest();
        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));
        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(Page.create(List.of(request)))));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC)).thenReturn(Mono.error(new IllegalStateException("request update failed")));
        AtomicBoolean batchCloseSubscribed = new AtomicBoolean(false);
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.defer(() -> {
            batchCloseSubscribed.set(true);
            return Mono.empty();
        }));

        StepVerifier.create(inadPollingService.handleMessage(buildPayload())).expectError(IllegalStateException.class).verify();

        assertFalse(batchCloseSubscribed.get());
        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC);
    }

    /**
     * Regression: se tutte le request sono state completate ma fallisce solo l'update finale del batch,
     * l'errore deve essere propagato dopo gli effetti sulle request; il replay potrà poi chiudere il batch vuoto.
     */
    @Test
    void handleMessage_shouldPropagateBatchCloseFailure_afterRequestsAreCompleted() {
        NationalRegistriesRequest request = buildRequest();
        ResponseStatusListDigitalAddress statusResponse = mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState()).thenReturn(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress resultResponse = mock(ResponseListDigitalAddress.class);
        when(inadClient.checkMassiveRequestStatus(POLLING_ID)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID)).thenReturn(Mono.just(resultResponse));
        when(inadConverter.mapResponseByTaxId(resultResponse)).thenReturn(Map.of());
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(Page.create(List.of(request)))));
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        when(pollingService.createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());
        IllegalStateException batchError = new IllegalStateException("batch update failed");
        when(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD)).thenReturn(Mono.error(batchError));

        StepVerifier.create(inadPollingService.handleMessage(buildPayload())).expectErrorMatches(error -> error == batchError).verify();

        verify(pollingService).createFallbackRequest(request, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC);
    }

    @Test
    void handleMessage_shouldProcessEachRequestOfTheBatchIndependently() {
        NationalRegistriesRequest found = buildRequest();
        found.setCorrelationId("corr-found");

        NationalRegistriesRequest missing = buildRequest();
        missing.setCorrelationId("corr-missing");
        missing.setTaxId("VRDLGI80A01H501X");

        ResponseStatusListDigitalAddress statusResponse =
                mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState())
                .thenReturn(StatusProcessingRequest.DISPONIBILE);

        ResponseListDigitalAddress resultResponse =
                mock(ResponseListDigitalAddress.class);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.just(resultResponse));

        when(inadConverter.mapResponseByTaxId(resultResponse))
                .thenReturn(Map.of(
                        TAX_ID,
                        buildInadResponse(
                                TAX_ID,
                                address("personal@pec.it", null)
                        )
                ));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(
                Flux.just(Page.create(List.of(found, missing)))
        ));

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow())
                .thenReturn(true);

        when(pollingService.createFallbackRequest(
                missing,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        when(gatewayUtils.convertCodeSqsDtoToString(any(CodeSqsDto.class)))
                .thenReturn("payload");

        when(pollingService.markRequestAsWorked(
                found,
                GatewayDownstreamService.INAD,
                "payload",
                true
        )).thenReturn(Mono.empty());

        when(pollingService.markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(inadPollingService.handleMessage(buildPayload()))
                .verifyComplete();

        verify(pollingService).markRequestAsWorked(
                found,
                GatewayDownstreamService.INAD,
                "payload",
                true
        );

        verify(pollingService).createFallbackRequest(
                missing,
                GatewayDownstreamService.INAD,
                GatewayDownstreamService.INIPEC
        );

        verify(pollingService, never()).markRequestAsWorked(
                eq(missing),
                any(),
                any(),
                anyBoolean()
        );
    }

    private void prepareAvailableResult(
            NationalRegistriesRequest request,
            ResponseRequestDigitalAddress inadResponse) {

        ResponseStatusListDigitalAddress statusResponse =
                mock(ResponseStatusListDigitalAddress.class);
        when(statusResponse.getState())
                .thenReturn(StatusProcessingRequest.DISPONIBILE);

        ResponseListDigitalAddress resultResponse =
                mock(ResponseListDigitalAddress.class);

        when(inadClient.checkMassiveRequestStatus(POLLING_ID))
                .thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(POLLING_ID))
                .thenReturn(Mono.just(resultResponse));

        when(inadConverter.mapResponseByTaxId(resultResponse))
                .thenReturn(Map.of(request.getTaxId(), inadResponse));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(
                Flux.just(Page.create(List.of(request)))
        ));

        when(pollingService.markBatchAsWorked(
                BATCH_ID,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());
    }

    private void mockPollingConfig() {
        NationalRegistriesConfig.Inad inad = new NationalRegistriesConfig.Inad();
        inad.setBatchPollingMaxRetry(5);
        inad.setBatchPollingInProgressMaxRetry(10);

        when(nationalRegistriesConfig.getInad())
                .thenReturn(inad);
    }

    private ResponseRequestDigitalAddress buildInadResponse(
            String taxId,
            ElementDigitalAddress... addresses) {

        ResponseRequestDigitalAddress response =
                new ResponseRequestDigitalAddress();

        response.setCodiceFiscale(taxId);
        response.setDigitalAddress(List.of(addresses));

        return response;
    }

    private ElementDigitalAddress address(
            String pec,
            String profession) {

        ElementDigitalAddress element =
                new ElementDigitalAddress();

        element.setDigitalAddress(pec);
        element.setPracticedProfession(profession);

        return element;
    }

    private PnPollingEvent.Payload buildPayload() {
        PnPollingEvent.Payload payload =
                new PnPollingEvent.Payload();

        payload.setBatchId(BATCH_ID);
        payload.setPollingId(POLLING_ID);
        payload.setBatchSize(1);
        payload.setErrorRetryCount(1);
        payload.setInProgressRetryCount(2);

        return payload;
    }

    private NationalRegistriesRequest buildRequest() {
        NationalRegistriesRequest request =
                new NationalRegistriesRequest();

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