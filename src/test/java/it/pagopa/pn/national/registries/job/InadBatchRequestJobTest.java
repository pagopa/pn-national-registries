package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InadBatchRequestJobTest {

    @Mock
    private NationalRegistriesRequestsRepository requestsRepository;
    @Mock
    private NationalRegistriesBatchesRepository batchesRepository;
    @Mock
    private InadClient inadClient;
    @Mock
    private Page<NationalRegistriesRequest> page;

    private InadBatchRequestJob job;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Inad inad = new NationalRegistriesConfig.Inad();
        inad.setMaxBatchRequestSize(3);
        inad.setOldestRequestMaxWaitingSeconds(60);
        inad.setPollingFirstAttemptDelaySecondsPerCf(1D);
        inad.setPollingFirstAttemptFixedDelaySeconds(10);

        NationalRegistriesConfig config = new NationalRegistriesConfig();
        config.setInad(inad);
        config.setQueryLimit(2);
        config.setTtl(3600);
        job = new InadBatchRequestJob(requestsRepository, batchesRepository, inadClient, config);
    }

    @Test
    void aggregatesRequestsAcrossPagesUntilBatchSizeIsReached() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:01:00Z"));
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.parse("2026-10-16T09:02:00Z"));
        Page<NationalRegistriesRequest> secondPage = mock(Page.class);
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first));
        when(secondPage.items()).thenReturn(List.of(second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page, secondPage)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K", "BNCLRA80A01H501M"), requestCaptor.getValue().getCodiciFiscali());

        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(3, batch.getBatchSize());
        verify(requestsRepository, times(3)).updateStatusToWorking(
                anyString(), anyString(), eq(batch.getBatchId()), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void sendsIncompleteBatchWhenOldestRequestHasReachedMaxWaitingTime() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now().minusSeconds(60));
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        verify(inadClient).callMassiveEService(any(RequestListDigitalAddress.class));
        verify(requestsRepository).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void leavesIncompleteRecentRequestsAvailableAfterAllPagesAreRead() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());

        when(page.items()).thenReturn(List.of(first));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesProviderErrorWithoutPersistingTheBatchOrChangingRequests() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(new IllegalStateException("INAD unavailable")));

        assertThrows(IllegalStateException.class, () -> job.batchRequest());

        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void sendsEachCompleteBatchAndReadsRemainingPages() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        NationalRegistriesRequest fourth = request("fourth", "NCLMRA80A01H501N", Instant.now());
        NationalRegistriesRequest fifth = request("fifth", "VRDGLI80A01H501P", Instant.now());
        NationalRegistriesRequest sixth = request("sixth", "BNCGLI80A01H501Q", Instant.now());
        Page<NationalRegistriesRequest> secondPage = mock(Page.class);
        Page<NationalRegistriesRequest> thirdPage = mock(Page.class);
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first, second));
        when(secondPage.items()).thenReturn(List.of(third, fourth));
        when(thirdPage.items()).thenReturn(List.of(fifth, sixth));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page, secondPage, thirdPage)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));
        verify(thirdPage).items();
    }

    private NationalRegistriesRequest request(String correlationId, String taxId, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setTaxId(taxId);
        request.setCreatedAt(createdAt);
        return request;
    }
}
