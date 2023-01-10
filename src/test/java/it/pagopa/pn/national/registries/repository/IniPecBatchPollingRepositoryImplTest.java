package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = IniPecBatchPollingRepositoryImpl.class)
class IniPecBatchPollingRepositoryImplTest {

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @MockBean
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testCreateBatchPolling(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);

        IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient);

        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setBatchId("batchId");
        batchPolling.setTimeStamp(LocalDateTime.now());
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setReservationId(null);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);

        when(dynamoDbAsyncTable.putItem(batchPolling)).thenReturn(completableFuture);

        StepVerifier.create(iniPecBatchPollingRepository.createBatchPolling(batchPolling)).expectNext(batchPolling).verifyComplete();
    }

    @Test
    void testUpdateBatchPolling(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);

        IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient);

        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setPollingId("pollingId");
        batchPolling.setBatchId("batchId");
        batchPolling.setTimeStamp(LocalDateTime.now());
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setReservationId(null);

        when(dynamoDbAsyncTable.updateItem(batchPolling)).thenReturn(CompletableFuture.completedFuture(batchPolling));

        StepVerifier.create(iniPecBatchPollingRepository.updateBatchPolling(batchPolling)).expectNext(batchPolling).verifyComplete();
    }

    @Test
    void testGetBatchPollingWithoutReservationIdAndStatusNotWork(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient);

        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        BatchPolling batchPolling = new BatchPolling();

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);

        when(dynamoDbAsyncTable.index(any())).thenReturn(index);

        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);

        Page<BatchPolling> page = Page.create(new ArrayList<>());

        StepVerifier.create(iniPecBatchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWork(lastKey))
                        .expectNext(page);
    }

    @Test
    void createSetReservationIdToAndStatusWorkingBatchPolling(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient);

        List<BatchPolling> batchPollings = new ArrayList<>();
        BatchPolling batchPolling = new BatchPolling();
        batchPollings.add(batchPolling);

        when(dynamoDbAsyncTable.updateItem(batchPolling)).thenReturn(CompletableFuture.completedFuture(batchPollings));

        PagePublisher<Object> pagePublisher = mock(PagePublisher.class);
        when(dynamoDbAsyncTable.scan((ScanEnhancedRequest) any())).thenReturn(pagePublisher);


        StepVerifier.create(iniPecBatchPollingRepository.setReservationIdToAndStatusWorkingBatchPolling(batchPollings,"reservationId"))
                .expectSubscription()
                .expectNext(batchPollings);
    }

    @Test
    void testGetBatchPollingByReservationIdAndStatusWorking(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchPollingRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchPollingRepositoryImpl(dynamoDbEnhancedAsyncClient);

        List<BatchPolling> batchPollings = new ArrayList<>();
        BatchPolling batchPolling = new BatchPolling();
        batchPollings.add(batchPolling);

        PagePublisher<Object> pagePublisher = mock(PagePublisher.class);
        when(dynamoDbAsyncTable.scan((ScanEnhancedRequest) any())).thenReturn(pagePublisher);


        StepVerifier.create(iniPecBatchPollingRepository.getBatchPollingByReservationIdAndStatusWorking("reservationId"))
                .expectSubscription()
                .expectNext(batchPollings);
    }

}
