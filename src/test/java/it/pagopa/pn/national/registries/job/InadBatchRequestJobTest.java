package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.BatchRequestService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InadBatchRequestJobTest {

    private static final int QUERY_LIMIT = 2;
    private static final int MAX_BATCH_SIZE = 3;
    private static final int MAX_RETRY = 3;
    private static final int RETRY_AFTER = 60;
    private static final GatewayDownstreamService REGISTRY = GatewayDownstreamService.INAD;

    @Mock private NationalRegistriesRequestsRepository requestsRepository;
    @Mock private InadClient inadClient;
    @Mock private BatchRequestService batchRequestService;
    @Mock private Page<NationalRegistriesRequest> page;

    private InadBatchRequestJob job;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inad inad = new NationalRegistriesConfig.Inad();
        inad.setMaxBatchRequestSize(MAX_BATCH_SIZE);
        inad.setOldestRequestMaxWaitingSeconds(60);
        inad.setPollingFirstAttemptDelaySecondsPerCf(1D);
        inad.setPollingFirstAttemptFixedDelaySeconds(10);
        inad.setBatchRequestMaxRetry(MAX_RETRY);
        inad.setRequestRetryAfter(RETRY_AFTER);
        NationalRegistriesConfig config = new NationalRegistriesConfig();
        config.setInad(inad);
        config.setQueryLimit(QUERY_LIMIT);
        job = new InadBatchRequestJob(requestsRepository, inadClient, config, batchRequestService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aggregatesRequestsAcrossPagesAndDelegatesPersistence() {
        List<NationalRegistriesRequest> requests = threeRequests();
        Page<NationalRegistriesRequest> secondPage = mock(Page.class);
        when(page.items()).thenReturn(List.of(requests.get(0)));
        when(secondPage.items()).thenReturn(requests.subList(1, 3));
        mockPages(page, secondPage);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> payload = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(payload.capture());
        assertEquals(requests.stream().map(NationalRegistriesRequest::getTaxId).toList(),
                payload.getValue().getCodiciFiscali());
        String batchId = payload.getValue().getPraticalReference();
        assertNotNull(batchId);
        assertTrue(batchId.endsWith("#0"));
        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> attempt = ArgumentCaptor.forClass(Instant.class);
        verify(batchRequestService).persistBatch(eq(batchId), eq("polling-id"), eq(requests),
                now.capture(), eq(REGISTRY), attempt.capture());
        assertEquals(now.getValue().plusSeconds(13).truncatedTo(ChronoUnit.SECONDS), attempt.getValue());
        verify(requestsRepository).getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT);
    }

    @Test
    void sendsIncompleteBatchWhenOldestRequestHasWaitedLongEnough() {
        NationalRegistriesRequest old = request("old", "CF1", Instant.now().minusSeconds(120));
        when(page.items()).thenReturn(List.of(old));
        mockPages(page);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(List.of(old)),
                any(Instant.class), eq(REGISTRY), any(Instant.class));
    }

    @Test
    void leavesRecentIncompleteBatchUnprocessed() {
        when(page.items()).thenReturn(List.of(request("recent", "CF1", Instant.now())));
        mockPages(page);
        job.batchRequest();
        verifyNoInteractions(inadClient, batchRequestService);
    }

    @Test
    void doesNotSendIncompleteBatchJustBelowWaitingThreshold() {
        when(page.items()).thenReturn(List.of(request("recent", "CF1", Instant.now().minusSeconds(30))));
        mockPages(page);
        job.batchRequest();
        verifyNoInteractions(inadClient, batchRequestService);
    }

    @Test
    void sendsIncompleteBatchWhenFirstRequestIsOld() {
        NationalRegistriesRequest old = request("old", "CF1", Instant.now().minusSeconds(120));
        NationalRegistriesRequest recent = request("recent", "CF2", Instant.now());
        when(page.items()).thenReturn(List.of(old, recent));
        mockPages(page);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(List.of(old, recent)),
                any(Instant.class), eq(REGISTRY), any(Instant.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsEachCompleteBatchAcrossPages() {
        List<NationalRegistriesRequest> requests = sixRequests();
        Page<NationalRegistriesRequest> page2 = mock(Page.class);
        Page<NationalRegistriesRequest> page3 = mock(Page.class);
        when(page.items()).thenReturn(requests.subList(0, 2));
        when(page2.items()).thenReturn(requests.subList(2, 4));
        when(page3.items()).thenReturn(requests.subList(4, 6));
        mockPages(page, page2, page3);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> payload = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient, times(2)).callMassiveEService(payload.capture());
        assertEquals(requests.subList(0, 3).stream().map(NationalRegistriesRequest::getTaxId).toList(),
                payload.getAllValues().get(0).getCodiciFiscali());
        assertEquals(requests.subList(3, 6).stream().map(NationalRegistriesRequest::getTaxId).toList(),
                payload.getAllValues().get(1).getCodiciFiscali());
        assertTrue(payload.getAllValues().get(0).getPraticalReference().endsWith("#0"));
        assertTrue(payload.getAllValues().get(1).getPraticalReference().endsWith("#1"));
        verify(batchRequestService, times(2)).persistBatch(anyString(), eq("polling-id"), anyList(),
                any(Instant.class), eq(REGISTRY), any(Instant.class));
    }

    @Test
    void skipsRecentTrailingIncompleteBatch() {
        List<NationalRegistriesRequest> requests = fourRequests();
        when(page.items()).thenReturn(requests);
        mockPages(page);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        verify(inadClient, times(1)).callMassiveEService(any(RequestListDigitalAddress.class));
        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(requests.subList(0, 3)),
                any(Instant.class), eq(REGISTRY), any(Instant.class));
    }

    @Test
    void sendsOldTrailingIncompleteBatch() {
        List<NationalRegistriesRequest> requests = fourRequests();
        NationalRegistriesRequest last = requests.get(3);
        last.setCreatedAt(Instant.now().minusSeconds(120));
        when(page.items()).thenReturn(requests);
        mockPages(page);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        mockSuccessfulPersistence();

        job.batchRequest();

        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));
        verify(batchRequestService).persistBatch(anyString(), eq("polling-id"), eq(List.of(last)),
                any(Instant.class), eq(REGISTRY), any(Instant.class));
    }

    @Test
    void noPagesDoesNothing() {
        mockPages();
        job.batchRequest();
        verifyNoInteractions(inadClient, batchRequestService);
    }

    @Test
    void emptyPageDoesNothing() {
        when(page.items()).thenReturn(List.of());
        mockPages(page);
        job.batchRequest();
        verifyNoInteractions(inadClient, batchRequestService);
    }

    @Test
    void badRequestDelegatesHandlingAndSkipsPersistence() {
        List<NationalRegistriesRequest> requests = threeRequests();
        mockFullBatch(requests);
        PnNationalRegistriesException providerError = providerException(400);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));
        mockSuccessfulErrorHandling(providerError, requests);

        assertDoesNotThrow(job::batchRequest);

        verify(batchRequestService).handleBatchError(providerError, requests, REGISTRY, MAX_RETRY, RETRY_AFTER);
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void mixedValidAndInvalidTaxIdsBadRequestDelegatesEntireBatch() {
        List<NationalRegistriesRequest> requests = List.of(
                request("valid", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z")),
                request("invalid", "rssmra80a01h501u", Instant.parse("2026-10-16T09:01:00Z")),
                request("valid2", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:02:00Z")));
        mockFullBatch(requests);
        PnNationalRegistriesException providerError = providerException(400);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));
        mockSuccessfulErrorHandling(providerError, requests);

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> payload = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(payload.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "rssmra80a01h501u", "VRDLGI80A01H501K"),
                payload.getValue().getCodiciFiscali());
        verify(batchRequestService).handleBatchError(providerError, requests, REGISTRY, MAX_RETRY, RETRY_AFTER);
    }

    @Test
    void nonBadRequestDelegatesRetryHandling() {
        List<NationalRegistriesRequest> requests = threeRequests();
        mockFullBatch(requests);
        PnNationalRegistriesException providerError = providerException(500);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));
        mockSuccessfulErrorHandling(providerError, requests);

        assertDoesNotThrow(job::batchRequest);

        verify(batchRequestService).handleBatchError(providerError, requests, REGISTRY, MAX_RETRY, RETRY_AFTER);
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void propagatesFailureOfErrorHandler() {
        List<NationalRegistriesRequest> requests = threeRequests();
        mockFullBatch(requests);
        PnNationalRegistriesException providerError = providerException(400);
        IllegalStateException updateError = new IllegalStateException("update failed");
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));
        when(batchRequestService.handleBatchError(same(providerError), eq(requests), eq(REGISTRY),
                eq(MAX_RETRY), eq(RETRY_AFTER))).thenReturn(Mono.error(updateError));

        assertSame(updateError, assertThrows(IllegalStateException.class, job::batchRequest));
        verify(batchRequestService, never()).persistBatch(anyString(), anyString(), anyList(), any(), any(), any());
    }

    @Test
    void propagatesGenericProviderError() {
        mockFullBatch(threeRequests());
        IllegalStateException providerError = new IllegalStateException("INAD unavailable");
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));

        assertSame(providerError, assertThrows(IllegalStateException.class, job::batchRequest));
        verifyNoInteractions(batchRequestService);
    }

    @Test
    void propagatesRepositoryReadError() {
        IllegalStateException repositoryError = new IllegalStateException("query failed");
        when(requestsRepository.getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
                .thenReturn(SdkPublisher.adapt(Flux.error(repositoryError)));

        assertSame(repositoryError, assertThrows(IllegalStateException.class, job::batchRequest));
        verifyNoInteractions(inadClient, batchRequestService);
    }

    @Test
    void emptyProviderResponseDoesNotPersist() {
        mockFullBatch(threeRequests());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.empty());

        assertDoesNotThrow(job::batchRequest);
        verifyNoInteractions(batchRequestService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingPollingIdPropagatesError(String pollingId) {
        mockFullBatch(threeRequests());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response(pollingId)));

        PnInternalException error = assertThrows(PnInternalException.class, job::batchRequest);
        assertTrue(error.getProblem().getDetail().contains("INAD returned no polling identifier for batch"));
        verifyNoInteractions(batchRequestService);
    }

    @Test
    void persistenceFailurePropagatesWithoutErrorHandling() {
        mockFullBatch(threeRequests());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(response("polling-id")));
        IllegalStateException persistenceError = new IllegalStateException("dynamo down");
        when(batchRequestService.persistBatch(anyString(), eq("polling-id"), anyList(),
                any(Instant.class), eq(REGISTRY), any(Instant.class)))
                .thenReturn(Mono.error(persistenceError));

        assertSame(persistenceError, assertThrows(IllegalStateException.class, job::batchRequest));
        verify(batchRequestService, never()).handleBatchError(any(), anyList(), any(), anyInt(), anyInt());
    }

    private void mockSuccessfulPersistence() {
        when(batchRequestService.persistBatch(anyString(), anyString(), anyList(),
                any(Instant.class), eq(REGISTRY), any(Instant.class))).thenReturn(Mono.empty());
    }

    private void mockSuccessfulErrorHandling(PnNationalRegistriesException error,
                                             List<NationalRegistriesRequest> requests) {
        when(batchRequestService.handleBatchError(same(error), eq(requests), eq(REGISTRY),
                eq(MAX_RETRY), eq(RETRY_AFTER))).thenReturn(Mono.empty());
    }

    private void mockFullBatch(List<NationalRegistriesRequest> requests) {
        when(page.items()).thenReturn(requests);
        mockPages(page);
    }

    @SafeVarargs
    private final void mockPages(Page<NationalRegistriesRequest>... pages) {
        when(requestsRepository.getRequestsByRegistryStatus(REGISTRY, RequestStatusEnum.NOT_WORKED, QUERY_LIMIT))
                .thenReturn(SdkPublisher.adapt(Flux.fromArray(pages)));
    }

    private static ResponseRequestListDigitalAddress response(String pollingId) {
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);
        return response;
    }

    private static PnNationalRegistriesException providerException(int status) {
        return new PnNationalRegistriesException("provider error", status, "status", new HttpHeaders(),
                new byte[0], StandardCharsets.UTF_8, Object.class);
    }

    private static List<NationalRegistriesRequest> threeRequests() {
        return List.of(
                request("first", "CF1", Instant.parse("2026-10-16T09:00:00Z")),
                request("second", "CF2", Instant.parse("2026-10-16T09:01:00Z")),
                request("third", "CF3", Instant.parse("2026-10-16T09:02:00Z")));
    }

    private static List<NationalRegistriesRequest> fourRequests() {
        return List.of(
                request("first", "CF1", Instant.now()),
                request("second", "CF2", Instant.now()),
                request("third", "CF3", Instant.now()),
                request("fourth", "CF4", Instant.now()));
    }

    private static List<NationalRegistriesRequest> sixRequests() {
        return List.of(
                request("first", "CF1", Instant.now()),
                request("second", "CF2", Instant.now()),
                request("third", "CF3", Instant.now()),
                request("fourth", "CF4", Instant.now()),
                request("fifth", "CF5", Instant.now()),
                request("sixth", "CF6", Instant.now()));
    }

    private static NationalRegistriesRequest request(String id, String taxId, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(id);
        request.setTaxId(taxId);
        request.setCreatedAt(createdAt);
        return request;
    }
}
