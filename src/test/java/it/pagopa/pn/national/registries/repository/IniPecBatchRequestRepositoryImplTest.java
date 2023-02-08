package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniPecBatchRequestRepositoryImplTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testUpdate() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchRequest batchRequest = new BatchRequest();

        when(dynamoDbAsyncTable.updateItem(same(batchRequest)))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.update(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testCreateBatchRequest() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        BatchRequest batchRequest = new BatchRequest();
        when(dynamoDbAsyncTable.putItem(batchRequest))
                .thenReturn(completableFuture);

        StepVerifier.create(batchRequestRepository.createBatchRequest(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testGetBatchRequestByNotBatchIdPageable() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(sdkPublisher);

        StepVerifier.create(batchRequestRepository.getBatchRequestByNotBatchId(lastKey, 100))
                .expectNextCount(0);
    }

    @Test
    void testGetBatchRequestsToSend() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.empty()));

        StepVerifier.create(batchRequestRepository.getBatchRequestsToSend("batchId"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testSetBatchRequestsStatus() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchRequest batchRequest = new BatchRequest();
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.setBatchRequestsStatus(batchRequest, "status"))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testSetNewBatchIdToBatchRequests() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchRequest batchRequest = new BatchRequest();

        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.setNewBatchIdToBatchRequest(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testGetBatchRequestToRecovery() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, 3);

        BatchRequest batchRequest = new BatchRequest();

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));

        StepVerifier.create(batchRequestRepository.getBatchRequestToRecovery())
                .expectNextCount(1)
                .verifyComplete();
    }
}
