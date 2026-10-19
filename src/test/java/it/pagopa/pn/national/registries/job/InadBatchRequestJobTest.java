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
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
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
        inad.setMaxBatchRequestSize(2);
        inad.setOldestRequestMaxWaitingSeconds(60);
        inad.setPollingFirstAttemptDelaySecondsPerCf(1D);
        inad.setPollingFirstAttemptFixedDelaySeconds(10);

        NationalRegistriesConfig config = new NationalRegistriesConfig();
        config.setInad(inad);
        config.setTtl(3600);
        job = new InadBatchRequestJob(requestsRepository, batchesRepository, inadClient, config);
    }

    @Test
    void sendsEligibleBatchAndAssociatesRequests() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:01:00Z"));
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first, second));
        when(requestsRepository.getRequestsByRegistryStatus(eq(GatewayDownstreamService.INAD), isNull(), eq(2)))
                .thenReturn(Mono.just(page), Mono.empty());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(ResponseEntity.accepted().body(providerResponse)));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K"), requestCaptor.getValue().getCodiciFiscali());

        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(2, batch.getBatchSize());
        verify(requestsRepository, times(2)).updateStatusToWorking(
                anyString(), anyString(), eq(batch.getBatchId()), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void leavesIncompleteRecentGroupAvailableForNextExecution() {
        NationalRegistriesRequest request = request("first", "RSSMRA80A01H501U", Instant.now());

        when(page.items()).thenReturn(List.of(request));
        when(requestsRepository.getRequestsByRegistryStatus(eq(GatewayDownstreamService.INAD), isNull(), eq(2)))
                .thenReturn(Mono.just(page));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesProviderErrorWithoutPersistingTheBatchOrChangingRequests() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());

        when(page.items()).thenReturn(List.of(first, second));
        when(requestsRepository.getRequestsByRegistryStatus(eq(GatewayDownstreamService.INAD), isNull(), eq(2)))
                .thenReturn(Mono.just(page));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(new IllegalStateException("INAD unavailable")));

        assertThrows(IllegalStateException.class, () -> job.batchRequest());

        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    private NationalRegistriesRequest request(String correlationId, String taxId, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setTaxId(taxId);
        request.setCreatedAt(createdAt);
        return request;
    }
}
