package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniPecBatchPollingRepositoryImplTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testUpdate() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepository batchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchPolling batchPolling = new BatchPolling();

        when(dynamoDbAsyncTable.updateItem(same(batchPolling)))
                .thenReturn(CompletableFuture.completedFuture(batchPolling));

        StepVerifier.create(batchPollingRepository.update(batchPolling))
                .expectNext(batchPolling)
                .verifyComplete();
    }

    @Test
    void testCreate() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepository batchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchPolling batchPolling = new BatchPolling();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        when(dynamoDbAsyncTable.putItem(batchPolling))
                .thenReturn(completableFuture);

        StepVerifier.create(batchPollingRepository.create(batchPolling))
                .expectNext(batchPolling)
                .verifyComplete();
    }

    @Test
    void testGetBatchPollingWithoutReservationIdAndStatusNotWorked() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepository iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(sdkPublisher);

        StepVerifier.create(iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(lastKey, 1))
                .expectNextCount(0);
    }

    @Test
    void testSetNewReservationIdToBatchPolling() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepository batchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchPolling batchPolling = new BatchPolling();

        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchPolling));

        StepVerifier.create(batchPollingRepository.setNewReservationIdToBatchPolling(batchPolling))
                .expectNext(batchPolling)
                .verifyComplete();
    }

    @Test
    void testGetBatchPollingToRecover() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepository batchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchPolling batchPolling = new BatchPolling();

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchPolling)))));

        StepVerifier.create(batchPollingRepository.getBatchPollingToRecover())
                .expectNextCount(1)
                .verifyComplete();
    }
}
