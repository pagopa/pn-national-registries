package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.entity.NationalRegistriesBatch.SK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    private static final String BATCH_ID = "batch-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String TAX_ID = "RSSMRA80A01H501U";
    private static final String CLIENT_ID = "client-id";
    private static final String REFERENCE_REQUEST_DATE = "2026-01-01";

    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @Mock
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Mock
    private NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    private InfoCamereService infoCamereService;

    private PollingService pollingService;

    @BeforeEach
    void setUp() {
        pollingService = new PollingService(
                nationalRegistriesRequestsRepository,
                nationalRegistriesBatchesRepository,
                nationalRegistriesConfig,
                infoCamereService
        );
    }

    @Test
    void incrementAndCheckRetry_shouldIncrementErrorRetry_whenErrorIsRetryable() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                2,
                false,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                1,
                                3,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("generic error")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        2,
                        false,
                        GatewayDownstreamService.INIPEC
                );

        verifyNoInteractions(nationalRegistriesRequestsRepository);
    }

    @Test
    void incrementAndCheckRetry_shouldIncrementInProgressRetry_whenPollingIsInProgress() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                4,
                true,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                2,
                                3,
                                true,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("polling not ready")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        4,
                        true,
                        GatewayDownstreamService.INIPEC
                );
    }

    @Test
    void incrementAndCheckRetry_shouldStartErrorRetryFromOne_whenRetryCountIsNull() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                1,
                false,
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                null,
                                null,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("error")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        1,
                        false,
                        GatewayDownstreamService.INIPEC
                );
    }

    @Test
    void incrementAndCheckRetry_shouldStartInProgressRetryFromOne_whenRetryCountIsNull() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                1,
                true,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                null,
                                null,
                                true,
                                5,
                                10,
                                GatewayDownstreamService.INAD,
                                null
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        1,
                        true,
                        GatewayDownstreamService.INAD
                );
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenErrorRetryIsExhausted() {
        NationalRegistriesRequest request = buildRequest();

        Page<NationalRegistriesRequest> page =
                Page.create(List.of(request));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.just(page)));

        when(nationalRegistriesRequestsRepository.updateStatusToError(
                CORRELATION_ID,
                CREATED_AT.toString(),
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.empty());

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                4,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("error")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToError(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        GatewayDownstreamService.INIPEC
                );

        verify(nationalRegistriesBatchesRepository)
                .updateToFinalStatus(
                        BATCH_ID,
                        SK,
                        BatchesStatusEnum.ERROR
                );

        verify(nationalRegistriesBatchesRepository, never())
                .resetStatusForRetry(
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyBoolean(),
                        any()
                );
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenInProgressRetryIsExhausted() {
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.empty()));

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                0,
                                9,
                                true,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("not ready")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .updateToFinalStatus(
                        BATCH_ID,
                        SK,
                        BatchesStatusEnum.ERROR
                );

        verify(nationalRegistriesBatchesRepository, never())
                .resetStatusForRetry(
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyBoolean(),
                        any()
                );
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenExceptionIsBadRequest() {
        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "bad request",
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        null,
                        null,
                        Charset.defaultCharset(),
                        Object.class
                );

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.empty()));

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                0,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                exception
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .updateToFinalStatus(
                        BATCH_ID,
                        SK,
                        BatchesStatusEnum.ERROR
                );

        verify(nationalRegistriesBatchesRepository, never())
                .resetStatusForRetry(
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyBoolean(),
                        any()
                );
    }

    @Test
    void incrementAndCheckRetry_shouldRetry_whenExceptionIsNotBadRequest() {
        PnNationalRegistriesException exception =
                new PnNationalRegistriesException(
                        "server error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        null,
                        null,
                        Charset.defaultCharset(),
                        Object.class
                );

        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                1,
                false,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                0,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INAD,
                                exception
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        1,
                        false,
                        GatewayDownstreamService.INAD
                );

        verify(nationalRegistriesBatchesRepository, never())
                .updateToFinalStatus(
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void incrementAndCheckRetry_shouldUseInProgressLimit_whenInProgress() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                3,
                true,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                5,
                                2,
                                true,
                                1,
                                10,
                                GatewayDownstreamService.INAD,
                                null
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .resetStatusForRetry(
                        BATCH_ID,
                        SK,
                        3,
                        true,
                        GatewayDownstreamService.INAD
                );

        verify(nationalRegistriesBatchesRepository, never())
                .updateToFinalStatus(
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void incrementAndCheckRetry_shouldPropagateError_whenResetStatusForRetryFails() {
        RuntimeException failure =
                new RuntimeException("dynamo error");

        when(nationalRegistriesBatchesRepository.resetStatusForRetry(
                BATCH_ID,
                SK,
                1,
                false,
                GatewayDownstreamService.INAD
        )).thenReturn(Mono.error(failure));

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                0,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INAD,
                                new RuntimeException("x")
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void incrementAndCheckRetry_shouldMoveAllWorkingRequestsToError_whenPollingFails() {
        NationalRegistriesRequest request1 = buildRequest();

        NationalRegistriesRequest request2 = buildRequest();
        request2.setCorrelationId("correlation-id-2");
        request2.setCreatedAt(
                Instant.parse("2026-01-01T11:00:00Z")
        );

        Page<NationalRegistriesRequest> page =
                Page.create(List.of(request1, request2));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.just(page)));

        when(nationalRegistriesRequestsRepository.updateStatusToError(
                anyString(),
                anyString(),
                eq(GatewayDownstreamService.INIPEC)
        )).thenReturn(Mono.empty());

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                4,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("error")
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToError(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        GatewayDownstreamService.INIPEC
                );

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToError(
                        "correlation-id-2",
                        "2026-01-01T11:00:00Z",
                        GatewayDownstreamService.INIPEC
                );
    }

    @Test
    void incrementAndCheckRetry_shouldPropagateError_whenMoveRequestToErrorFails() {
        NationalRegistriesRequest request = buildRequest();

        Page<NationalRegistriesRequest> page =
                Page.create(List.of(request));

        RuntimeException failure =
                new RuntimeException("request update failed");

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.just(page)));

        when(nationalRegistriesRequestsRepository.updateStatusToError(
                CORRELATION_ID,
                CREATED_AT.toString(),
                GatewayDownstreamService.INIPEC
        )).thenReturn(Mono.error(failure));

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                4,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("polling failed")
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void incrementAndCheckRetry_shouldPropagateError_whenFinalErrorBatchUpdateFails() {
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(
                BATCH_ID,
                RequestStatusEnum.WORKING
        )).thenReturn(SdkPublisher.adapt(Flux.empty()));

        RuntimeException failure =
                new RuntimeException("batch update failed");

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.ERROR
        )).thenReturn(Mono.error(failure));

        StepVerifier.create(
                        pollingService.incrementAndCheckRetry(
                                BATCH_ID,
                                4,
                                0,
                                false,
                                5,
                                10,
                                GatewayDownstreamService.INIPEC,
                                new RuntimeException("polling failed")
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void createFallbackRequest_shouldCreateTargetRequestAndMarkSourceAsWorked() {
        NationalRegistriesRequest sourceRequest = buildRequest();

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(false);

        when(nationalRegistriesRequestsRepository.putRequest(
                any(NationalRegistriesRequest.class)
        )).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INIPEC,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INIPEC,
                                GatewayDownstreamService.INAD
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .putRequest(argThat(request ->
                        request.getRegistry() == GatewayDownstreamService.INAD
                                && request.getStatus() == RequestStatusEnum.NOT_WORKED
                ));

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToWorked(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        null,
                        GatewayDownstreamService.INIPEC,
                        false
                );

        verifyNoInteractions(infoCamereService);
    }

    @Test
    void createFallbackRequest_shouldCreateInipecRequest_whenOldInipecFlowIsDisabled() {
        NationalRegistriesRequest sourceRequest = buildInadRequest();

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(false);

        when(nationalRegistriesRequestsRepository.putRequest(
                any(NationalRegistriesRequest.class)
        )).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INAD,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INAD,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .putRequest(argThat(request ->
                        CORRELATION_ID.equals(request.getCorrelationId())
                                && TAX_ID.equals(request.getTaxId())
                                && request.getRegistry() == GatewayDownstreamService.INIPEC
                                && request.getStatus() == RequestStatusEnum.NOT_WORKED
                ));

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToWorked(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        null,
                        GatewayDownstreamService.INAD,
                        false
                );

        verifyNoInteractions(infoCamereService);
    }

    @Test
    void createFallbackRequest_shouldUseOldInipecFlow_whenEnabledAndTargetIsInipec() {
        NationalRegistriesRequest sourceRequest = buildInadRequest();

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(true);

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CLIENT_ID),
                any(),
                any(Date.class),
                eq(RecipientType.PF)
        )).thenReturn(Mono.empty());

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INAD,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INAD,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .verifyComplete();

        ArgumentCaptor<Date> dateCaptor =
                ArgumentCaptor.forClass(Date.class);

        verify(infoCamereService)
                .getIniPecDigitalAddress(
                        eq(CLIENT_ID),
                        argThat(request ->
                                request != null
                        ),
                        dateCaptor.capture(),
                        eq(RecipientType.PF)
                );

        Date expectedDate = Date.from(
                LocalDate.parse(REFERENCE_REQUEST_DATE)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
        );

        assertEquals(
                expectedDate.toInstant(),
                dateCaptor.getValue().toInstant()
        );

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToWorked(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        null,
                        GatewayDownstreamService.INAD,
                        false
                );

        verify(nationalRegistriesRequestsRepository, never())
                .putRequest(any(NationalRegistriesRequest.class));
    }

    @Test
    void createFallbackRequest_shouldNotUseOldInipecFlow_whenTargetIsInad() {
        NationalRegistriesRequest sourceRequest = buildRequest();

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(true);

        when(nationalRegistriesRequestsRepository.putRequest(
                any(NationalRegistriesRequest.class)
        )).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INIPEC,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INIPEC,
                                GatewayDownstreamService.INAD
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .putRequest(argThat(request ->
                        request.getRegistry() == GatewayDownstreamService.INAD
                                && request.getStatus() == RequestStatusEnum.NOT_WORKED
                ));

        verifyNoInteractions(infoCamereService);
    }

    @Test
    void createFallbackRequest_shouldPropagateError_whenOldInipecCallFails() {
        NationalRegistriesRequest sourceRequest = buildInadRequest();

        RuntimeException failure =
                new RuntimeException("inipec error");

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(true);

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CLIENT_ID),
                any(),
                any(Date.class),
                eq(RecipientType.PF)
        )).thenReturn(Mono.error(failure));

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INAD,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INAD,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();

        verify(nationalRegistriesRequestsRepository, never())
                .putRequest(any(NationalRegistriesRequest.class));
    }

    @Test
    void createFallbackRequest_shouldPropagateError_whenCreatingFallbackRequestFails() {
        NationalRegistriesRequest sourceRequest = buildInadRequest();

        RuntimeException failure =
                new RuntimeException("put request failed");

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(false);

        when(nationalRegistriesRequestsRepository.putRequest(
                any(NationalRegistriesRequest.class)
        )).thenReturn(Mono.error(failure));

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INAD,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INAD,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();

        verifyNoInteractions(infoCamereService);
    }

    @Test
    void createFallbackRequest_shouldPropagateError_whenMarkingSourceAsWorkedFails() {
        NationalRegistriesRequest sourceRequest = buildInadRequest();

        RuntimeException failure =
                new RuntimeException("source update failed");

        when(nationalRegistriesConfig.isEnableOldInipecFlow())
                .thenReturn(false);

        when(nationalRegistriesRequestsRepository.putRequest(
                any(NationalRegistriesRequest.class)
        )).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INAD,
                false
        )).thenReturn(Mono.error(failure));

        StepVerifier.create(
                        pollingService.createFallbackRequest(
                                sourceRequest,
                                GatewayDownstreamService.INAD,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();

        verify(nationalRegistriesRequestsRepository)
                .putRequest(any(NationalRegistriesRequest.class));

        verifyNoInteractions(infoCamereService);
    }

    @Test
    void markRequestAsWorked_shouldUpdateRequestWithPayloadAndPublishFlag() {
        NationalRegistriesRequest request = buildRequest();

        String payload = "{\"result\":\"ok\"}";

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                payload,
                GatewayDownstreamService.INIPEC,
                true
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.markRequestAsWorked(
                                request,
                                GatewayDownstreamService.INIPEC,
                                payload,
                                true
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToWorked(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        payload,
                        GatewayDownstreamService.INIPEC,
                        true
                );
    }

    @Test
    void markRequestAsWorked_shouldPassNullPayloadAndNoPublish() {
        NationalRegistriesRequest request = buildRequest();

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INIPEC,
                false
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.markRequestAsWorked(
                                request,
                                GatewayDownstreamService.INIPEC,
                                null,
                                false
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository)
                .updateStatusToWorked(
                        CORRELATION_ID,
                        CREATED_AT.toString(),
                        null,
                        GatewayDownstreamService.INIPEC,
                        false
                );
    }

    @Test
    void markRequestAsWorked_shouldPropagateError_whenRepositoryFails() {
        NationalRegistriesRequest request = buildRequest();

        RuntimeException failure =
                new RuntimeException("request update failed");

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(
                CORRELATION_ID,
                CREATED_AT.toString(),
                null,
                GatewayDownstreamService.INIPEC,
                false
        )).thenReturn(Mono.error(failure));

        StepVerifier.create(
                        pollingService.markRequestAsWorked(
                                request,
                                GatewayDownstreamService.INIPEC,
                                null,
                                false
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void markBatchAsWorked_shouldUpdateBatchToWorked() {
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.WORKED
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        pollingService.markBatchAsWorked(
                                BATCH_ID,
                                GatewayDownstreamService.INIPEC
                        )
                )
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository)
                .updateToFinalStatus(
                        BATCH_ID,
                        SK,
                        BatchesStatusEnum.WORKED
                );
    }

    @Test
    void markBatchAsWorked_shouldPropagateError_whenUpdateFails() {
        RuntimeException failure =
                new RuntimeException("dynamo error");

        when(nationalRegistriesBatchesRepository.updateToFinalStatus(
                BATCH_ID,
                SK,
                BatchesStatusEnum.WORKED
        )).thenReturn(Mono.error(failure));

        StepVerifier.create(
                        pollingService.markBatchAsWorked(
                                BATCH_ID,
                                GatewayDownstreamService.INAD
                        )
                )
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    private NationalRegistriesRequest buildRequest() {
        NationalRegistriesRequest request =
                new NationalRegistriesRequest();

        request.setCorrelationId(CORRELATION_ID);
        request.setTaxId(TAX_ID);
        request.setCreatedAt(CREATED_AT);
        request.setRegistry(GatewayDownstreamService.INIPEC);
        request.setStatus(RequestStatusEnum.WORKING);

        return request;
    }

    private NationalRegistriesRequest buildInadRequest() {
        NationalRegistriesRequest request =
                new NationalRegistriesRequest();

        request.setCorrelationId(CORRELATION_ID);
        request.setTaxId(TAX_ID);
        request.setClientId(CLIENT_ID);
        request.setReferenceRequestDate(REFERENCE_REQUEST_DATE);
        request.setRecipientType(RecipientType.PF.name());
        request.setCreatedAt(CREATED_AT);
        request.setRegistry(GatewayDownstreamService.INAD);
        request.setStatus(RequestStatusEnum.WORKING);

        return request;
    }
}