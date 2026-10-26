package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingJobTest {

    @Mock
    private NationalRegistriesBatchesRepository repository;

    @InjectMocks
    private PollingJob pollingJob;

    @Test
    void startJobWithoutBatchesToProcess() {
        Page<NationalRegistriesBatch> page = mock(Page.class);

        when(page.items()).thenReturn(List.of());
        when(repository.getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class))).thenReturn(SdkPublisher.adapt(Flux.just(page)));

        pollingJob.polling();

        verify(repository).getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class));
        verify(repository, never()).updateItem(any(), any());
    }

    @Test
    void startJobWith1BatchToProcess() {
        NationalRegistriesBatch batch = batch("batch-1", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));

        Page<NationalRegistriesBatch> page = mock(Page.class);

        when(page.items()).thenReturn(List.of(batch));

        when(repository.getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class))).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(repository.updateItem(any(NationalRegistriesBatch.class), eq(BatchesStatusEnum.WORKING))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        pollingJob.polling();

        ArgumentCaptor<NationalRegistriesBatch> captor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(repository).updateItem(captor.capture(), eq(BatchesStatusEnum.WORKING));
        NationalRegistriesBatch updatedBatch = captor.getValue();
        assertEquals("batch-1", updatedBatch.getBatchId());
    }

    @Test
    void startJobWith3BatchToProcessInOnePage() {
        NationalRegistriesBatch batch1 = batch("batch-1", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch2 = batch("batch-2", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch3 = batch("batch-3", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));

        Page<NationalRegistriesBatch> page = mock(Page.class);
        when(page.items()).thenReturn(List.of(batch1, batch2, batch3));
        when(repository.getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class))).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(repository.updateItem(any(NationalRegistriesBatch.class), eq(BatchesStatusEnum.WORKING))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        pollingJob.polling();

        ArgumentCaptor<NationalRegistriesBatch> captor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(repository, times(3)).updateItem(captor.capture(), eq(BatchesStatusEnum.WORKING));
        assertEquals(
                Set.of("batch-1", "batch-2", "batch-3"),
                captor.getAllValues()
                        .stream()
                        .map(NationalRegistriesBatch::getBatchId)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void startJobWith4BatchToProcessInTwoPages() {
        NationalRegistriesBatch batch1 = batch("batch-1", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch2 = batch("batch-2", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch3 = batch("batch-3", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch4 = batch("batch-4", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));

        Page<NationalRegistriesBatch> firstPage = mock(Page.class);
        Page<NationalRegistriesBatch> secondPage = mock(Page.class);

        when(firstPage.items()).thenReturn(List.of(batch1, batch2));
        when(secondPage.items()).thenReturn(List.of(batch3, batch4));

        when(repository.getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class))).thenReturn(SdkPublisher.adapt(Flux.just(firstPage, secondPage)));
        when(repository.updateItem(any(NationalRegistriesBatch.class), eq(BatchesStatusEnum.WORKING))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        pollingJob.polling();

        verify(repository, times(1)).getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class));
        ArgumentCaptor<NationalRegistriesBatch> captor = ArgumentCaptor.forClass(NationalRegistriesBatch.class);
        verify(repository, times(4)).updateItem(captor.capture(), eq(BatchesStatusEnum.WORKING));
        assertEquals(
                Set.of("batch-1", "batch-2", "batch-3", "batch-4"),
                captor.getAllValues()
                        .stream()
                        .map(NationalRegistriesBatch::getBatchId)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void startJobWith3BatchToProcessInOnePageWithOneInvalidStatus() {
        NationalRegistriesBatch batch1 = batch("batch-1", BatchesStatusEnum.WORKING, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch2 = batch("batch-2", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));
        NationalRegistriesBatch batch3 = batch("batch-3", BatchesStatusEnum.REQUEST_SENT, Instant.parse("2024-06-01T10:00:00Z"));

        Page<NationalRegistriesBatch> page = mock(Page.class);

        when(page.items()).thenReturn(List.of(batch1, batch2, batch3));

        when(repository.getBatchesToProcess(eq(BatchesStatusEnum.REQUEST_SENT), any(Instant.class))).thenReturn(SdkPublisher.adapt(Flux.just(page)));
        when(repository.updateItem(
                argThat(batch ->
                        batch != null
                                && "batch-1".equals(batch.getBatchId())
                ),
                eq(BatchesStatusEnum.WORKING)
        )).thenReturn(Mono.error(ConditionalCheckFailedException.builder().message("Condition failed").build()));

        when(repository.updateItem(
                argThat(batch ->
                        batch != null
                                && (
                                "batch-2".equals(batch.getBatchId())
                                        || "batch-3".equals(batch.getBatchId())
                        )
                ),
                eq(BatchesStatusEnum.WORKING)
        )).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        assertDoesNotThrow(() -> pollingJob.polling());

        verify(repository, times(3)).updateItem(any(NationalRegistriesBatch.class), eq(BatchesStatusEnum.WORKING));
    }

    private NationalRegistriesBatch batch(String id, BatchesStatusEnum status, Instant attemptAfter) {
        NationalRegistriesBatch nationalRegistriesBatch = new NationalRegistriesBatch();
        nationalRegistriesBatch.setStatus(status);
        nationalRegistriesBatch.setBatchId(id);
        nationalRegistriesBatch.setAttemptAfter(formatInstant(attemptAfter));
        return nationalRegistriesBatch;
    }
}
