package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;

import static it.pagopa.pn.national.registries.entity.NationalRegistriesBatch.SK;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    private static final String BATCH_ID = "batch-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @Mock
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    private PollingService pollingService;

    @BeforeEach
    void setUp() {
        pollingService = new PollingService(
                nationalRegistriesRequestsRepository,
                nationalRegistriesBatchesRepository
        );
    }

    @Test
    void incrementAndCheckRetry_shouldIncrementErrorRetry_whenErrorIsRetryable() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 2, false, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                1,
                3,
                false,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("generic error")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 2, false, GatewayDownstreamService.INIPEC);
        verifyNoInteractions(nationalRegistriesRequestsRepository);
    }

    @Test
    void incrementAndCheckRetry_shouldIncrementInProgressRetry_whenPollingIsInProgress() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 4, true, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                2,
                3,
                true,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("polling not ready")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 4, true, GatewayDownstreamService.INIPEC);
    }

    @Test
    void incrementAndCheckRetry_shouldStartFromOne_whenRetryCountIsNull() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 1, false, GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                null,
                null,
                false,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("error")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 1, false, GatewayDownstreamService.INIPEC);
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenErrorRetryIsExhausted() {
        NationalRegistriesRequest request = buildRequest();

        Page<NationalRegistriesRequest> page = Page.create(List.of(request));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesRequestsRepository.updateStatusToError(CORRELATION_ID, CREATED_AT.toString(), GatewayDownstreamService.INIPEC)).thenReturn(Mono.empty());
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                4,
                0,
                false,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("error")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesRequestsRepository).updateStatusToError(CORRELATION_ID, CREATED_AT.toString(), GatewayDownstreamService.INIPEC);
        verify(nationalRegistriesBatchesRepository).updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR);
        verify(nationalRegistriesBatchesRepository, never()).resetStatusForRetry(anyString(), anyString(), anyInt(), anyBoolean(), any());
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenInProgressRetryIsExhausted() {
        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.empty()));
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                0,
                9,
                true,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("not ready")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR);
        verify(nationalRegistriesBatchesRepository, never()).resetStatusForRetry(anyString(), anyString(), anyInt(), anyBoolean(), any());
    }

    @Test
    void incrementAndCheckRetry_shouldHandlePollingFailure_whenExceptionIsBadRequest() {
        PnNationalRegistriesException exception =
                new PnNationalRegistriesException("bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), Object.class);

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.empty()));
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                0,
                0,
                false,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                exception
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR);
        verify(nationalRegistriesBatchesRepository, never()).resetStatusForRetry(anyString(), anyString(), anyInt(), anyBoolean(), any());
    }

    @Test
    void createFallbackRequest_shouldCreateTargetRequestAndMarkSourceAsWorked() {
        NationalRegistriesRequest sourceRequest = buildRequest();

        when(nationalRegistriesRequestsRepository.putRequest(any(NationalRegistriesRequest.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(nationalRegistriesRequestsRepository.updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INIPEC, false)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.createFallbackRequest(
                sourceRequest,
                GatewayDownstreamService.INIPEC,
                GatewayDownstreamService.INAD
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesRequestsRepository).putRequest(
                argThat(request ->
                        request.getRegistry() == GatewayDownstreamService.INAD
                                && request.getStatus() == RequestStatusEnum.NOT_WORKED
                )
        );

        verify(nationalRegistriesRequestsRepository).updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INIPEC, false);
    }

    @Test
    void markRequestAsWorked_shouldUpdateRequestWithPayloadAndPublishFlag() {
        NationalRegistriesRequest request = buildRequest();
        String payload = "{\"result\":\"ok\"}";

        when(nationalRegistriesRequestsRepository.updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), payload, GatewayDownstreamService.INIPEC, true)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.markRequestAsWorked(
                request,
                GatewayDownstreamService.INIPEC,
                payload,
                true
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesRequestsRepository).updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), payload, GatewayDownstreamService.INIPEC, true);
    }

    @Test
    void markBatchAsWorked_shouldUpdateBatchToWorked() {
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.WORKED)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INIPEC);

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesBatchesRepository).updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.WORKED);
    }

    @Test
    void incrementAndCheckRetry_shouldMoveAllWorkingRequestsToError_whenPollingFails() {
        NationalRegistriesRequest request1 = buildRequest();

        NationalRegistriesRequest request2 = buildRequest();
        request2.setCorrelationId("correlation-id-2");
        request2.setCreatedAt(Instant.parse("2026-01-01T11:00:00Z"));

        Page<NationalRegistriesRequest> page = Page.create(List.of(request1, request2));

        when(nationalRegistriesRequestsRepository.getRequestsBatchId(BATCH_ID, RequestStatusEnum.WORKING)).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(nationalRegistriesRequestsRepository.updateStatusToError(anyString(), anyString(), eq(GatewayDownstreamService.INIPEC))).thenReturn(Mono.empty());
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.ERROR)).thenReturn(Mono.empty());

        Mono<Void> result = pollingService.incrementAndCheckRetry(
                BATCH_ID,
                4,
                0,
                false,
                5,
                10,
                GatewayDownstreamService.INIPEC,
                new RuntimeException("error")
        );

        StepVerifier.create(result).verifyComplete();

        verify(nationalRegistriesRequestsRepository).updateStatusToError(CORRELATION_ID, CREATED_AT.toString(), GatewayDownstreamService.INIPEC);
        verify(nationalRegistriesRequestsRepository).updateStatusToError("correlation-id-2", "2026-01-01T11:00:00Z", GatewayDownstreamService.INIPEC);
    }

    @Test
    void incrementAndCheckRetry_shouldRetry_whenInProgressWithNullThrowable() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 1, true, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(pollingService.incrementAndCheckRetry(BATCH_ID, 0, 0, true, 5, 10, GatewayDownstreamService.INAD, null))
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 1, true, GatewayDownstreamService.INAD);
        verifyNoInteractions(nationalRegistriesRequestsRepository);
    }

    @Test
    void incrementAndCheckRetry_shouldRetry_whenExceptionIsNotBadRequest() {
        PnNationalRegistriesException exception =
                new PnNationalRegistriesException("server error", HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null, null, Charset.defaultCharset(), Object.class);
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 1, false, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        StepVerifier.create(pollingService.incrementAndCheckRetry(BATCH_ID, 0, 0, false, 5, 10, GatewayDownstreamService.INAD, exception))
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 1, false, GatewayDownstreamService.INAD);
        verify(nationalRegistriesBatchesRepository, never()).updateToFinalStatus(anyString(), anyString(), any());
    }

    @Test
    void incrementAndCheckRetry_shouldUseInProgressLimitAndIgnoreErrorLimit_whenInProgress() {
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 3, true, GatewayDownstreamService.INAD)).thenReturn(Mono.empty());

        // errore max = 1 (esaurito se usato per errore), in-progress max = 10: deve essere usato il limite in-progress
        StepVerifier.create(pollingService.incrementAndCheckRetry(BATCH_ID, 5, 2, true, 1, 10, GatewayDownstreamService.INAD, null))
                .verifyComplete();

        verify(nationalRegistriesBatchesRepository).resetStatusForRetry(BATCH_ID, SK, 3, true, GatewayDownstreamService.INAD);
    }

    @Test
    void incrementAndCheckRetry_shouldPropagateError_whenResetStatusForRetryFails() {
        RuntimeException failure = new RuntimeException("dynamo error");
        when(nationalRegistriesBatchesRepository.resetStatusForRetry(BATCH_ID, SK, 1, false, GatewayDownstreamService.INAD)).thenReturn(Mono.error(failure));

        StepVerifier.create(pollingService.incrementAndCheckRetry(BATCH_ID, 0, 0, false, 5, 10, GatewayDownstreamService.INAD, new RuntimeException("x")))
                .expectErrorMatches(e -> e == failure)
                .verify();
    }

    @Test
    void createFallbackRequest_shouldPutRequestWithPreservedDataAndTargetRegistryStatus() {
        NationalRegistriesRequest sourceRequest = buildRequest();
        sourceRequest.setTaxId("RSSMRA80A01H501U");
        when(nationalRegistriesRequestsRepository.putRequest(any(NationalRegistriesRequest.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(nationalRegistriesRequestsRepository.updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INAD, false)).thenReturn(Mono.empty());

        StepVerifier.create(pollingService.createFallbackRequest(sourceRequest, GatewayDownstreamService.INAD, GatewayDownstreamService.INIPEC))
                .verifyComplete();

        verify(nationalRegistriesRequestsRepository).putRequest(argThat(request ->
                CORRELATION_ID.equals(request.getCorrelationId())
                        && "RSSMRA80A01H501U".equals(request.getTaxId())
                        && request.getRegistry() == GatewayDownstreamService.INIPEC
                        && request.getStatus() == RequestStatusEnum.NOT_WORKED));
        verify(nationalRegistriesRequestsRepository).updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INAD, false);
    }

    @Test
    void markRequestAsWorked_shouldPassNullPayloadAndNoPublish() {
        NationalRegistriesRequest request = buildRequest();
        when(nationalRegistriesRequestsRepository.updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INIPEC, false)).thenReturn(Mono.empty());

        StepVerifier.create(pollingService.markRequestAsWorked(request, GatewayDownstreamService.INIPEC, null, false)).verifyComplete();

        verify(nationalRegistriesRequestsRepository).updateStatusToWorked(CORRELATION_ID, CREATED_AT.toString(), null, GatewayDownstreamService.INIPEC, false);
    }

    @Test
    void markBatchAsWorked_shouldPropagateError_whenUpdateFails() {
        RuntimeException failure = new RuntimeException("dynamo error");
        when(nationalRegistriesBatchesRepository.updateToFinalStatus(BATCH_ID, SK, BatchesStatusEnum.WORKED)).thenReturn(Mono.error(failure));

        StepVerifier.create(pollingService.markBatchAsWorked(BATCH_ID, GatewayDownstreamService.INAD))
                .expectErrorMatches(e -> e == failure)
                .verify();
    }

    private NationalRegistriesRequest buildRequest() {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(CORRELATION_ID);
        request.setCreatedAt(CREATED_AT);
        request.setRegistry(GatewayDownstreamService.INIPEC);
        request.setStatus(RequestStatusEnum.WORKING);
        return request;
    }
}
