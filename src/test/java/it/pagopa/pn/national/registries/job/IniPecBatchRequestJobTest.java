package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IniPecBatchRequestJobTest {

    private static final int QUERY_LIMIT = 10;
    private static final int MAX_BATCH_SIZE = 2;

    @Mock
    private NationalRegistriesRequestsRepository requestsRepository;
    @Mock
    private NationalRegistriesBatchesRepository batchesRepository;
    @Mock
    private InfoCamereService infoCamereService;
    @Mock
    private InfoCamereConverter infoCamereConverter;
    @Mock
    private Page<NationalRegistriesRequest> page;

    private IniPecBatchRequestJob job;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setMaxBatchRequestSize(MAX_BATCH_SIZE);

        NationalRegistriesConfig config = new NationalRegistriesConfig();
        config.setInipec(inipec);
        config.setQueryLimit(QUERY_LIMIT);

        job = new IniPecBatchRequestJob(requestsRepository, batchesRepository, config, infoCamereService, infoCamereConverter);
    }

    @Test
    void doesNothingWhenThereAreNoRequests() {
        when(page.items()).thenReturn(List.of());
        mockPages(page);

        job.batchRequestScheduled();

        verifyNoInteractions(infoCamereConverter, infoCamereService, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void doesNothingWhenRepositoryReturnsNoPages() {
        mockPages();

        job.batchRequestScheduled();

        verifyNoInteractions(infoCamereConverter, infoCamereService, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void submitsBatchAndMarksRequestsAsWorking() {
        NationalRegistriesRequest first = request("c1", "RSSMRA80A01H501U", Instant.parse("2026-01-01T10:00:00Z"));
        NationalRegistriesRequest second = request("c2", "VRDLGI80A01H501K", Instant.parse("2026-01-01T10:01:00Z"));
        when(page.items()).thenReturn(List.of(first, second));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        Instant attemptAfter = Instant.parse("2026-01-01T10:10:00Z");
        when(infoCamereConverter.createIniPecRequest(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K"))).thenReturn(iniPecRequest);
        when(infoCamereConverter.calculateFirstAttemptAfter(2)).thenReturn(attemptAfter);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(first));

        job.batchRequestScheduled();

        verify(infoCamereConverter).createIniPecRequest(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K"));
        verify(infoCamereConverter).calculateFirstAttemptAfter(2);

        ArgumentCaptor<String> batchIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(infoCamereService).callEService(eq(iniPecRequest), batchIdCaptor.capture());

        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();
        assertNotNull(batch.getBatchId());
        assertEquals(batchIdCaptor.getValue(), batch.getBatchId());
        assertEquals(batch.getBatchId(), batch.getPracticalReferenceValue());
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(GatewayDownstreamService.INIPEC.name(), batch.getRegistry());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(2, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getCreatedAt());
        assertEquals(batch.getCreatedAt(), batch.getUpdatedAt());
        assertEquals(GatewayUtils.formatInstant(attemptAfter), batch.getAttemptAfter());

        verify(requestsRepository).updateStatusToWorking("c1", first.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository).updateStatusToWorking("c2", second.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository, times(2)).updateStatusToWorking(anyString(), anyString(), eq(batch.getBatchId()), eq(GatewayDownstreamService.INIPEC));
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void submitsIncompleteBatchWhenLessRequestsThanMaxBatchSizeAreAvailable() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        Instant attemptAfter = Instant.parse("2026-01-01T10:10:00Z");
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereConverter.calculateFirstAttemptAfter(1)).thenReturn(attemptAfter);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(first));

        job.batchRequestScheduled();

        verify(infoCamereConverter).createIniPecRequest(List.of("CF1"));
        verify(infoCamereConverter).calculateFirstAttemptAfter(1);
        verify(infoCamereService).callEService(eq(iniPecRequest), anyString());

        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();
        assertEquals(1, batch.getBatchSize());
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(GatewayUtils.formatInstant(attemptAfter), batch.getAttemptAfter());

        verify(requestsRepository).updateStatusToWorking("c1", first.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INIPEC);
    }

    @Test
    void readsRequestsWithConfiguredLimitAndRegistryStatus() {
        when(page.items()).thenReturn(List.of());
        mockPages(page);

        job.batchRequestScheduled();

        verify(requestsRepository).getRequestsByRegistryStatus(GatewayDownstreamService.INIPEC, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT);
    }

    @Test
    void limitsRequestsToMaxBatchRequestSizeAcrossPages() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        NationalRegistriesRequest second = request("c2", "CF2", Instant.parse("2026-01-01T10:01:00Z"));
        NationalRegistriesRequest third = request("c3", "CF3", Instant.parse("2026-01-01T10:02:00Z"));
        Page<NationalRegistriesRequest> secondPage = mock(Page.class);
        when(page.items()).thenReturn(List.of(first));
        when(secondPage.items()).thenReturn(List.of(second, third));
        mockPages(page, secondPage);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2"))).thenReturn(iniPecRequest);
        when(infoCamereConverter.calculateFirstAttemptAfter(2)).thenReturn(Instant.parse("2026-01-01T10:10:00Z"));
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(first));

        job.batchRequestScheduled();

        verify(infoCamereConverter).createIniPecRequest(List.of("CF1", "CF2"));
        verify(infoCamereConverter).calculateFirstAttemptAfter(2);
        verify(infoCamereService).callEService(eq(iniPecRequest), anyString());
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, times(2)).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INIPEC));
        verify(requestsRepository, never()).updateStatusToWorking(eq("c3"), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void failsWhenProviderReturnsNoPollingIdentifier(String pollingId) {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response(pollingId)));

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> job.batchRequestScheduled());

        assertEquals("INIPEC returned no polling identifier", thrown.getProblem().getDetail());
        verifyNoInteractions(batchesRepository);
        verify(infoCamereConverter, never()).calculateFirstAttemptAfter(anyInt());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void marksAllRequestsAsErrorWhenProviderReturnsBadRequest() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        NationalRegistriesRequest second = request("c2", "CF2", Instant.parse("2026-01-01T10:01:00Z"));
        when(page.items()).thenReturn(List.of(first, second));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.error(pnException(400)));
        when(requestsRepository.updateStatusToError(anyString(), anyString(), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.just(first));

        assertDoesNotThrow(() -> job.batchRequestScheduled());

        verify(requestsRepository).updateStatusToError("c1", first.getCreatedAt().toString(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository).updateStatusToError("c2", second.getCreatedAt().toString(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository, times(2)).updateStatusToError(anyString(), anyString(), eq(GatewayDownstreamService.INIPEC));
        verifyNoInteractions(batchesRepository);
        verify(infoCamereConverter, never()).calculateFirstAttemptAfter(anyInt());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesErrorWhenMarkingRequestsAsErrorFails() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        NationalRegistriesRequest second = request("c2", "CF2", Instant.parse("2026-01-01T10:01:00Z"));
        IllegalStateException updateError = new IllegalStateException("update error failed");
        when(page.items()).thenReturn(List.of(first, second));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.error(pnException(400)));
        when(requestsRepository.updateStatusToError("c1", first.getCreatedAt().toString(), GatewayDownstreamService.INIPEC))
                .thenReturn(Mono.error(updateError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequestScheduled());

        assertSame(updateError, thrown);
        verify(requestsRepository).updateStatusToError("c1", first.getCreatedAt().toString(), GatewayDownstreamService.INIPEC);
        verify(requestsRepository, never()).updateStatusToError(eq("c2"), anyString(), eq(GatewayDownstreamService.INIPEC));
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesProviderErrorDifferentFromBadRequest() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        PnNationalRegistriesException providerError = pnException(500);
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.error(providerError));

        PnNationalRegistriesException thrown = assertThrows(PnNationalRegistriesException.class, () -> job.batchRequestScheduled());

        assertSame(providerError, thrown);
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(batchesRepository);
    }

    @Test
    void propagatesGenericProviderError() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        IllegalStateException providerError = new IllegalStateException("unavailable");
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString()))
                .thenReturn(Mono.error(providerError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequestScheduled());

        assertSame(providerError, thrown);
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(batchesRepository);
    }

    @Test
    void completesWithoutPersistingWhenProviderReturnsEmptyMono() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> job.batchRequestScheduled());

        verify(infoCamereService).callEService(eq(iniPecRequest), anyString());
        verifyNoInteractions(batchesRepository);
        verify(infoCamereConverter, never()).calculateFirstAttemptAfter(anyInt());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void doesNotUpdateRequestsWhenBatchPersistenceFails() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        IllegalStateException persistenceError = new IllegalStateException("dynamo down");
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(iniPecRequest);
        when(infoCamereConverter.calculateFirstAttemptAfter(1)).thenReturn(Instant.parse("2026-01-01T10:10:00Z"));
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenReturn(Mono.error(persistenceError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequestScheduled());

        assertSame(persistenceError, thrown);
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void propagatesErrorWhenUpdatingRequestToWorkingFails() {
        NationalRegistriesRequest first = request("c1", "CF1", Instant.parse("2026-01-01T10:00:00Z"));
        NationalRegistriesRequest second = request("c2", "CF2", Instant.parse("2026-01-01T10:01:00Z"));
        IllegalStateException updateError = new IllegalStateException("update working failed");
        when(page.items()).thenReturn(List.of(first, second));
        mockPages(page);

        IniPecBatchRequest iniPecRequest = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2"))).thenReturn(iniPecRequest);
        when(infoCamereConverter.calculateFirstAttemptAfter(2)).thenReturn(Instant.parse("2026-01-01T10:10:00Z"));
        when(infoCamereService.callEService(eq(iniPecRequest), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(eq("c1"), eq(first.getCreatedAt().toString()), anyString(), eq(GatewayDownstreamService.INIPEC)))
                .thenReturn(Mono.error(updateError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequestScheduled());

        assertSame(updateError, thrown);
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository).updateStatusToWorking(eq("c1"), eq(first.getCreatedAt().toString()), anyString(), eq(GatewayDownstreamService.INIPEC));
        verify(requestsRepository, never()).updateStatusToWorking(eq("c2"), anyString(), anyString(), eq(GatewayDownstreamService.INIPEC));
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void propagatesErrorWhenReadingRequestsFails() {
        IllegalStateException repositoryError = new IllegalStateException("query failed");
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INIPEC, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
                .thenReturn(SdkPublisher.adapt(Flux.error(repositoryError)));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequestScheduled());

        assertSame(repositoryError, thrown);
        verifyNoInteractions(infoCamereConverter, infoCamereService, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @SafeVarargs
    private void mockPages(Page<NationalRegistriesRequest>... pages) {
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INIPEC, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
                .thenReturn(SdkPublisher.adapt(Flux.fromArray(pages)));
    }

    private static IniPecBatchResponse response(String pollingId) {
        IniPecBatchResponse response = new IniPecBatchResponse();
        response.setIdentificativoRichiesta(pollingId);
        return response;
    }

    private static PnNationalRegistriesException pnException(int status) {
        return new PnNationalRegistriesException("error", status, "status", new HttpHeaders(),
                new byte[0], StandardCharsets.UTF_8, InfoCamereClient.class);
    }

    private static NationalRegistriesRequest request(String correlationId, String taxId, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setTaxId(taxId);
        request.setCreatedAt(createdAt);
        return request;
    }
}