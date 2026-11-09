package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.springframework.http.HttpHeaders;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
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
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
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
        assertNotNull(requestCaptor.getValue().getPraticalReference());

        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();
        assertEquals(requestCaptor.getValue().getPraticalReference(), batch.getBatchId());
        assertEquals(batch.getBatchId(), batch.getPracticalReferenceValue());
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(3, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getAttemptAfter());
        assertNotNull(batch.getCreatedAt());
        assertEquals(batch.getCreatedAt(), batch.getUpdatedAt());
        verify(requestsRepository, times(3)).updateStatusToWorking(
                anyString(), anyString(), eq(batch.getBatchId()), eq(GatewayDownstreamService.INAD));
        verify(requestsRepository).getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2);
    }

    @Test
    void sendsIncompleteBatchWhenOldestRequestHasReachedMaxWaitingTime() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now().minusSeconds(60));
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED,2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        verify(inadClient).callMassiveEService(any(RequestListDigitalAddress.class));
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void leavesIncompleteRecentRequestsAvailableAfterAllPagesAreRead() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());

        when(page.items()).thenReturn(List.of(first));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void propagatesProviderErrorWithoutPersistingTheBatchOrChangingRequests() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        IllegalStateException providerError = new IllegalStateException("INAD unavailable");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequest());

        assertSame(providerError, thrown);
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void propagatesRepositoryErrorWithoutCallingProvider() {
        IllegalStateException repositoryError = new IllegalStateException("dynamo query failed");

        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.error(repositoryError)));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequest());

        assertSame(repositoryError, thrown);
        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
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
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page, secondPage, thirdPage)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient, times(2)).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K", "BNCLRA80A01H501M"), requestCaptor.getAllValues().get(0).getCodiciFiscali());
        assertEquals(List.of("NCLMRA80A01H501N", "VRDGLI80A01H501P", "BNCGLI80A01H501Q"), requestCaptor.getAllValues().get(1).getCodiciFiscali());
        verify(batchesRepository, times(2)).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, times(6)).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
        verify(thirdPage).items();
    }

    @Test
    void sendsOnlyCompleteBatchWhenTrailingIncompleteBatchIsRecent() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        NationalRegistriesRequest fourth = request("fourth", "NCLMRA80A01H501N", Instant.now());
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first, second, third, fourth));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
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
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, times(3)).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
        verify(requestsRepository, never()).updateStatusToWorking(eq("fourth"), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void sendsTrailingIncompleteBatchWhenItsOldestRequestHasReachedMaxWaitingTime() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        NationalRegistriesRequest fourth = request("fourth", "NCLMRA80A01H501N", Instant.now().minusSeconds(120));
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first, second, third, fourth));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient, times(2)).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K", "BNCLRA80A01H501M"), requestCaptor.getAllValues().get(0).getCodiciFiscali());
        assertEquals(List.of("NCLMRA80A01H501N"), requestCaptor.getAllValues().get(1).getCodiciFiscali());
        verify(batchesRepository, times(2)).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, times(4)).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void doesNothingWhenThereAreNoRequestsToProcess() {
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.empty()));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void doesNothingWhenPageHasNoItems() {
        when(page.items()).thenReturn(List.of());
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void doesNotSendIncompleteBatchWhenOldestRequestIsJustBelowMaxWaitingTime() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now().minusSeconds(50));

        when(page.items()).thenReturn(List.of(first));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));

        job.batchRequest();

        verifyNoInteractions(inadClient, batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void sendsIncompleteBatchWhenOnlyTheFirstRequestIsOldEnough() {
        NationalRegistriesRequest oldest = request("oldest", "RSSMRA80A01H501U", Instant.now().minusSeconds(120));
        NationalRegistriesRequest recent = request("recent", "VRDLGI80A01H501K", Instant.now());
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(oldest, recent));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(oldest));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K"), requestCaptor.getValue().getCodiciFiscali());
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, times(2)).updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD));
    }

    @Test
    void batchIdIsUsedAsPracticalReferenceAndPersistedBatchIsConsistent() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:01:00Z"));
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.parse("2026-10-16T09:02:00Z"));
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        Instant before = Instant.now();
        job.batchRequest();
        Instant after = Instant.now();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        ArgumentCaptor<NationalRegistriesBatch> batchCaptor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(batchesRepository).putBatch(batchCaptor.capture());
        NationalRegistriesBatch batch = batchCaptor.getValue();

        assertNotNull(batch.getBatchId());
        assertEquals(batch.getBatchId(), requestCaptor.getValue().getPraticalReference());
        assertEquals(batch.getBatchId(), batch.getPracticalReferenceValue());
        assertEquals(List.of("RSSMRA80A01H501U", "VRDLGI80A01H501K", "BNCLRA80A01H501M"), requestCaptor.getValue().getCodiciFiscali());
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(3, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getCreatedAt());
        assertEquals(batch.getCreatedAt(), batch.getUpdatedAt());

        // delay primo polling = 1.0 s per CF * 3 CF + 10 s fisso = 13 s (troncato al secondo)
        Instant attemptAfter = Instant.parse(batch.getAttemptAfter());
        assertFalse(attemptAfter.isBefore(before.plusSeconds(13).minusSeconds(1)));
        assertFalse(attemptAfter.isAfter(after.plusSeconds(13)));

        verify(requestsRepository).updateStatusToWorking("first", first.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToWorking("second", second.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToWorking("third", third.getCreatedAt().toString(), batch.getBatchId(), GatewayDownstreamService.INAD);
    }

    @Test
    void marksAllRequestsOfTheBatchAsErrorWhenProviderReturnsBadRequest() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:01:00Z"));
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.parse("2026-10-16T09:02:00Z"));

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerException(400)));
        when(requestsRepository.updateStatusToError(anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenReturn(Mono.just(first));

        job.batchRequest();

        verify(requestsRepository).updateStatusToError("first", first.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToError("second", second.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToError("third", third.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    /**
     * N21 - rende esplicito il comportamento di un batch misto: se il provider rifiuta l'intero batch per un CF problematico,
     * anche le richieste formalmente valide dello stesso invio vengono riportate nello stato di errore e nessun batch viene persistito.
     */
    @Test
    void mixedValidAndInvalidTaxIds_providerBadRequest_shouldMarkEveryRequestAsError() {
        NationalRegistriesRequest validPf = request("valid-pf", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest invalidLowercase = request("invalid-lowercase", "rssmra80a01h501u", Instant.parse("2026-10-16T09:01:00Z"));
        NationalRegistriesRequest validSecond = request("valid-second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:02:00Z"));

        when(page.items()).thenReturn(List.of(validPf, invalidLowercase, validSecond));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.error(providerException(400)));
        when(requestsRepository.updateStatusToError(anyString(), anyString(), eq(GatewayDownstreamService.INAD))).thenReturn(Mono.just(validPf));

        job.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of("RSSMRA80A01H501U", "rssmra80a01h501u", "VRDLGI80A01H501K"), requestCaptor.getValue().getCodiciFiscali());
        verify(requestsRepository).updateStatusToError("valid-pf", validPf.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToError("invalid-lowercase", invalidLowercase.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verify(requestsRepository).updateStatusToError("valid-second", validSecond.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }


    @Test
    void propagatesErrorWhenMarkingBadRequestAsErrorFails() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.parse("2026-10-16T09:00:00Z"));
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.parse("2026-10-16T09:01:00Z"));
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.parse("2026-10-16T09:02:00Z"));
        IllegalStateException updateError = new IllegalStateException("update error failed");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.error(providerException(400)));
        when(requestsRepository.updateStatusToError("first", first.getCreatedAt().toString(), GatewayDownstreamService.INAD))
                .thenReturn(Mono.error(updateError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequest());

        assertSame(updateError, thrown);
        verify(requestsRepository).updateStatusToError("first", first.getCreatedAt().toString(), GatewayDownstreamService.INAD);
        verify(requestsRepository, never()).updateStatusToError(eq("second"), anyString(), eq(GatewayDownstreamService.INAD));
        verify(requestsRepository, never()).updateStatusToError(eq("third"), anyString(), eq(GatewayDownstreamService.INAD));
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesNonBadRequestProviderErrorWithoutChangingRequests() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        PnNationalRegistriesException providerError = providerException(500);

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.error(providerError));

        PnNationalRegistriesException thrown = assertThrows(PnNationalRegistriesException.class, () -> job.batchRequest());

        assertSame(providerError, thrown);
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(batchesRepository);
    }

    @Test
    void completesWithoutPersistingWhenProviderReturnsEmptyMono() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.empty());

        assertDoesNotThrow(() -> job.batchRequest());

        verify(inadClient).callMassiveEService(any(RequestListDigitalAddress.class));
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void failsWithoutPersistingWhenProviderReturnsNoPollingId() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(new ResponseRequestListDigitalAddress()));

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> job.batchRequest());

        assertTrue(thrown.getProblem().getDetail().contains("INAD returned no polling identifier for batch"));
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
        verify(requestsRepository, never()).updateStatusToError(anyString(), anyString(), any());
    }

    @Test
    void failsWithoutPersistingWhenProviderReturnsBlankPollingId() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("  ");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(providerResponse));

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> job.batchRequest());

        assertTrue(thrown.getProblem().getDetail().contains("INAD returned no polling identifier for batch"));
        verifyNoInteractions(batchesRepository);
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesErrorWhenBatchCannotBePersisted() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");
        IllegalStateException persistenceError = new IllegalStateException("dynamo down");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenReturn(Mono.error(persistenceError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequest());

        assertSame(persistenceError, thrown);
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository, never()).updateStatusToWorking(anyString(), anyString(), anyString(), any());
    }

    @Test
    void propagatesErrorWhenRequestCannotBeMarkedAsWorking() {
        NationalRegistriesRequest first = request("first", "RSSMRA80A01H501U", Instant.now());
        NationalRegistriesRequest second = request("second", "VRDLGI80A01H501K", Instant.now());
        NationalRegistriesRequest third = request("third", "BNCLRA80A01H501M", Instant.now());
        ResponseRequestListDigitalAddress providerResponse = new ResponseRequestListDigitalAddress();
        providerResponse.setId("polling-id");
        IllegalStateException updateError = new IllegalStateException("request update failed");

        when(page.items()).thenReturn(List.of(first, second, third));
        when(requestsRepository.getRequestsByRegistryStatus(GatewayDownstreamService.INAD, RequestStatusEnum.NOT_WORKED, 2))
                .thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(providerResponse));
        when(batchesRepository.putBatch(any(NationalRegistriesBatch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(requestsRepository.updateStatusToWorking(anyString(), anyString(), anyString(), eq(GatewayDownstreamService.INAD)))
                .thenAnswer(invocation -> {
                    String correlationId = invocation.getArgument(0);
                    if ("first".equals(correlationId)) {
                        return Mono.error(updateError);
                    }
                    return Mono.just(first);
                });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> job.batchRequest());

        assertSame(updateError, thrown);
        verify(batchesRepository).putBatch(any(NationalRegistriesBatch.class));
        verify(requestsRepository).updateStatusToWorking(
                eq("first"),
                eq(first.getCreatedAt().toString()),
                anyString(),
                eq(GatewayDownstreamService.INAD)
        );
    }

    private PnNationalRegistriesException providerException(int status) {
        return new PnNationalRegistriesException("provider error", status, "status", new HttpHeaders(),
                new byte[0], java.nio.charset.StandardCharsets.UTF_8, Object.class);
    }

    private NationalRegistriesRequest request(String correlationId, String taxId, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setTaxId(taxId);
        request.setCreatedAt(createdAt);
        return request;
    }
}