package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterRepositoryImplTest {

    @Mock
    private DynamoDbAsyncTable<Object> table;
    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Test
    void getCounter() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(table);
        CounterRepository counterRepository = new CounterRepositoryImpl(dynamoDbEnhancedAsyncClient, "");
        when(table.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(new CounterModel()));
        StepVerifier.create(counterRepository.getCounter(""))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void createUpdateItemEnhancedRequest() {
        CounterRepositoryImpl counterRepository = new CounterRepositoryImpl(dynamoDbEnhancedAsyncClient, "");
        UpdateItemEnhancedRequest<CounterModel> upd = counterRepository.createUpdateItemEnhancedRequest("anpr");
        assertNotNull(upd);
    }
}
