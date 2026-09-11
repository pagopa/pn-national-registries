package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Collections;
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
    @Mock
    private NationalRegistriesConfig nationalRegistriesConfig;
    private static final int RETRY = 3;
    private static final int AFTER = 60;
    private IniPecBatchRequestRepository batchRequestRepository;

    @BeforeEach
    void setUp() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        batchRequestRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, nationalRegistriesConfig);
    }

    @Test
    void testUpdate() {
        BatchRequest batchRequest = new BatchRequest();

        when(dynamoDbAsyncTable.updateItem(same(batchRequest)))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.update(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testCreate() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        BatchRequest batchRequest = new BatchRequest();
        when(dynamoDbAsyncTable.putItem(batchRequest))
                .thenReturn(completableFuture);

        StepVerifier.create(batchRequestRepository.create(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testGetBatchRequestByNotBatchId() {
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
    void testGetBatchRequestByBatchId() {
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(Collections.emptyList()))));
        when(nationalRegistriesConfig.getQueryLimit()).thenReturn(1000);

        StepVerifier.create(batchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.WORKING, new HashMap<>()))
                .expectNextMatches(page -> page.items().isEmpty())
                .verifyComplete();
    }

    @Test
    void testSetNewBatchIdToBatchRequests() {
        BatchRequest batchRequest = new BatchRequest();

        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.setNewBatchIdToBatchRequest(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testSetNewReservationIdToBatchRequest() {
        BatchRequest batchRequest = new BatchRequest();

        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchRequestRepository.setNewReservationIdToBatchRequest(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

    @Test
    void testGetBatchRequestToRecovery() {
        BatchRequest batchRequest = new BatchRequest();

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        NationalRegistriesConfig.Inipec inipecConfig = new NationalRegistriesConfig.Inipec();
        inipecConfig.setBatchRequestMaxRetry(RETRY);
        inipecConfig.setBatchRequestRecoveryAfter(AFTER);

        when(nationalRegistriesConfig.getInipec())
                .thenReturn(inipecConfig);

        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));

        StepVerifier.create(batchRequestRepository.getBatchRequestToRecovery())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testGetBatchRequestToSend() {
        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        NationalRegistriesConfig.Inipec inipecConfig = new NationalRegistriesConfig.Inipec();
        inipecConfig.setBatchRequestMaxRetry(RETRY);
        inipecConfig.setBatchRequestRecoveryAfter(AFTER);

        when(nationalRegistriesConfig.getInipec())
                .thenReturn(inipecConfig);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(sdkPublisher);

        StepVerifier.create(batchRequestRepository.getBatchRequestToSend(lastKey, 100))
                .expectNextCount(0);
    }
}
