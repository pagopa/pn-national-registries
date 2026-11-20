package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.BatchRequestService;
import it.pagopa.pn.national.registries.service.InfoCamereService;
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
    private static final int MAX_RETRY = 3;
    private static final int RETRY_AFTER = 60;
    private static final GatewayDownstreamService REGISTRY = GatewayDownstreamService.INIPEC;

    @Mock private NationalRegistriesRequestsRepository requestsRepository;
    @Mock private InfoCamereService infoCamereService;
    @Mock private InfoCamereConverter infoCamereConverter;
    @Mock private BatchRequestService batchRequestService;
    @Mock private Page<NationalRegistriesRequest> page;

    private IniPecBatchRequestJob job;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setMaxBatchRequestSize(MAX_BATCH_SIZE);
        inipec.setBatchRequestMaxRetry(MAX_RETRY);
        inipec.setRequestRetryAfter(RETRY_AFTER);
        NationalRegistriesConfig config = new NationalRegistriesConfig();
        config.setInipec(inipec);
        config.setQueryLimit(QUERY_LIMIT);
        job = new IniPecBatchRequestJob(requestsRepository, config, infoCamereService,
                infoCamereConverter, batchRequestService);
    }

    @Test
    void doesNothingWhenThereAreNoRequests() {
        when(page.items()).thenReturn(List.of());
        mockPages(page);
        job.batchRequestScheduled();
        verifyNoInteractions(infoCamereConverter, infoCamereService, batchRequestService);
    }

    @Test
    void doesNothingWhenRepositoryReturnsNoPages() {
        mockPages();
        job.batchRequestScheduled();
        verifyNoInteractions(infoCamereConverter, infoCamereService, batchRequestService);
    }

    @Test
    void submitsBatchAndDelegatesPersistence() {
        NationalRegistriesRequest first = request("c1", "RSSMRA80A01H501U", "2026-01-01T10:00:00Z");
        NationalRegistriesRequest second = request("c2", "VRDLGI80A01H501K", "2026-01-01T10:01:00Z");
        List<NationalRegistriesRequest> requests = List.of(first, second);
        when(page.items()).thenReturn(requests);
        mockPages(page);
        IniPecBatchRequest payload = new IniPecBatchRequest();
        Instant attemptAfter = Instant.parse("2026-01-01T10:10:00Z");
        when(infoCamereConverter.createIniPecRequest(List.of(first.getTaxId(), second.getTaxId())))
                .thenReturn(payload);
        when(infoCamereService.callEService(eq(payload), anyString()))
                .thenReturn(Mono.just(response("polling-id")));
        when(infoCamereConverter.calculateFirstAttemptAfter(2)).thenReturn(attemptAfter);
        mockSuccessfulPersistence();

        job.batchRequestScheduled();

        ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
        verify(infoCamereService).callEService(eq(payload), id.capture());
        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        verify(batchRequestService).persistBatch(eq(id.getValue()), eq("polling-id"), eq(requests),
                now.capture(), eq(REGISTRY), eq(attemptAfter));
        assertNotNull(now.getValue());
        verify(infoCamereConverter).createIniPecRequest(List.of(first.getTaxId(), second.getTaxId()));
        verify(infoCamereConverter).calculateFirstAttemptAfter(2);
        verify(batchRequestService, never()).handleBatchError(any(), anyList(), any(), anyInt(), anyInt());
    }

    @Test
    void submitsIncompleteBatch() {
        NationalRegistriesRequest first = request("c1", "CF1", "2026-01-01T10:00:00Z");
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);
        IniPecBatchRequest payload = new IniPecBatchRequest();
        Instant attemptAfter = Instant.parse("2026-01-01T10:10:00Z");
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(payload);
        when(infoCamereService.callEService(eq(payload), anyString())).thenReturn(Mono.just(response("polling-id")));
        when(infoCamereConverter.calculateFirstAttemptAfter(1)).thenReturn(attemptAfter);
        mockSuccessfulPersistence();

        job.batchRequestScheduled();

        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(List.of(first)),
                any(Instant.class), eq(REGISTRY), eq(attemptAfter));
    }

    @Test
    void readsRequestsWithConfiguredLimitAndRegistryStatus() {
        mockPages();
        job.batchRequestScheduled();
        verify(requestsRepository).getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void limitsRequestsToMaxBatchRequestSizeAcrossPages() {
        NationalRegistriesRequest first = request("c1", "CF1", "2026-01-01T10:00:00Z");
        NationalRegistriesRequest second = request("c2", "CF2", "2026-01-01T10:01:00Z");
        NationalRegistriesRequest third = request("c3", "CF3", "2026-01-01T10:02:00Z");
        Page<NationalRegistriesRequest> secondPage = mock(Page.class);
        when(page.items()).thenReturn(List.of(first));
        when(secondPage.items()).thenReturn(List.of(second, third));
        mockPages(page, secondPage);
        IniPecBatchRequest payload = new IniPecBatchRequest();
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2"))).thenReturn(payload);
        when(infoCamereService.callEService(eq(payload), anyString())).thenReturn(Mono.just(response("polling-id")));
        Instant attemptAfter = Instant.parse("2026-01-01T10:10:00Z");
        when(infoCamereConverter.calculateFirstAttemptAfter(2)).thenReturn(attemptAfter);
        mockSuccessfulPersistence();

        job.batchRequestScheduled();

        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(List.of(first, second)),
                any(Instant.class), eq(REGISTRY), eq(attemptAfter));
        verify(infoCamereConverter, never()).createIniPecRequest(List.of("CF1", "CF2", "CF3"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void failsWhenProviderReturnsNoPollingIdentifier(String pollingId) {
        prepareOneRequest();
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.just(response(pollingId)));

        PnInternalException error = assertThrows(PnInternalException.class, job::batchRequestScheduled);
        assertEquals("INIPEC returned no polling identifier", error.getProblem().getDetail());
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
        verifyNoInteractions(batchRequestService);
        verify(infoCamereConverter, never()).calculateFirstAttemptAfter(anyInt());
    }

    @Test
    void badRequestDelegatesErrorHandlingAndSkipsPersistence() {
        List<NationalRegistriesRequest> requests = prepareTwoRequests();
        PnNationalRegistriesException providerError = pnException(400);
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.error(providerError));
        when(batchRequestService.handleBatchError(same(providerError), eq(requests), eq(REGISTRY),
                eq(MAX_RETRY), eq(RETRY_AFTER))).thenReturn(Mono.empty());

        assertDoesNotThrow(job::batchRequestScheduled);

        verify(batchRequestService).handleBatchError(providerError, requests, REGISTRY, MAX_RETRY, RETRY_AFTER);
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void nonBadRequestAlsoDelegatesToRetryHandling() {
        List<NationalRegistriesRequest> requests = prepareTwoRequests();
        PnNationalRegistriesException providerError = pnException(500);
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.error(providerError));
        when(batchRequestService.handleBatchError(same(providerError), eq(requests), eq(REGISTRY),
                eq(MAX_RETRY), eq(RETRY_AFTER))).thenReturn(Mono.empty());

        assertDoesNotThrow(job::batchRequestScheduled);

        verify(batchRequestService).handleBatchError(providerError, requests, REGISTRY, MAX_RETRY, RETRY_AFTER);
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void propagatesErrorWhenErrorHandlerFails() {
        List<NationalRegistriesRequest> requests = prepareTwoRequests();
        PnNationalRegistriesException providerError = pnException(400);
        IllegalStateException handlerError = new IllegalStateException("update error failed");
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.error(providerError));
        when(batchRequestService.handleBatchError(same(providerError), eq(requests), eq(REGISTRY),
                eq(MAX_RETRY), eq(RETRY_AFTER))).thenReturn(Mono.error(handlerError));

        assertSame(handlerError, assertThrows(IllegalStateException.class, job::batchRequestScheduled));
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void propagatesGenericProviderError() {
        prepareOneRequest();
        IllegalStateException providerError = new IllegalStateException("unavailable");
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.error(providerError));

        assertSame(providerError, assertThrows(IllegalStateException.class, job::batchRequestScheduled));
        verifyNoInteractions(batchRequestService);
    }

    @Test
    void emptyProviderResponseDoesNotPersist() {
        prepareOneRequest();
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.empty());

        assertDoesNotThrow(job::batchRequestScheduled);
        verifyNoInteractions(batchRequestService);
        verify(infoCamereConverter, never()).calculateFirstAttemptAfter(anyInt());
    }

    @Test
    void propagatesPersistenceFailure() {
        prepareOneRequest();
        when(infoCamereService.callEService(any(IniPecBatchRequest.class), anyString()))
                .thenReturn(Mono.just(response("polling-id")));
        when(infoCamereConverter.calculateFirstAttemptAfter(1))
                .thenReturn(Instant.parse("2026-01-01T10:10:00Z"));
        IllegalStateException error = new IllegalStateException("dynamo down");
        when(batchRequestService.persistBatch(anyString(), eq("polling-id"), anyList(),
                any(Instant.class), eq(REGISTRY), any(Instant.class))).thenReturn(Mono.error(error));

        assertSame(error, assertThrows(IllegalStateException.class, job::batchRequestScheduled));
        verify(batchRequestService, never()).handleBatchError(any(), anyList(), any(), anyInt(), anyInt());
    }

    @Test
    void propagatesRepositoryReadFailure() {
        IllegalStateException error = new IllegalStateException("query failed");
        when(requestsRepository.getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
                .thenReturn(SdkPublisher.adapt(Flux.error(error)));

        assertSame(error, assertThrows(IllegalStateException.class, job::batchRequestScheduled));
        verifyNoInteractions(infoCamereConverter, infoCamereService, batchRequestService);
    }

    private void mockSuccessfulPersistence() {
        when(batchRequestService.persistBatch(anyString(), anyString(), anyList(),
                any(Instant.class), eq(REGISTRY), any(Instant.class))).thenReturn(Mono.empty());
    }

    private void prepareOneRequest() {
        NationalRegistriesRequest first = request("c1", "CF1", "2026-01-01T10:00:00Z");
        when(page.items()).thenReturn(List.of(first));
        mockPages(page);
        when(infoCamereConverter.createIniPecRequest(List.of("CF1"))).thenReturn(new IniPecBatchRequest());
    }

    private List<NationalRegistriesRequest> prepareTwoRequests() {
        List<NationalRegistriesRequest> requests = List.of(
                request("c1", "CF1", "2026-01-01T10:00:00Z"),
                request("c2", "CF2", "2026-01-01T10:01:00Z"));
        when(page.items()).thenReturn(requests);
        mockPages(page);
        when(infoCamereConverter.createIniPecRequest(List.of("CF1", "CF2")))
                .thenReturn(new IniPecBatchRequest());
        return requests;
    }

    @SafeVarargs
    private final void mockPages(Page<NationalRegistriesRequest>... pages) {
        when(requestsRepository.getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
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

    private static NationalRegistriesRequest request(String id, String taxId, String createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(id);
        request.setTaxId(taxId);
        request.setCreatedAt(Instant.parse(createdAt));
        return request;
    }
}
