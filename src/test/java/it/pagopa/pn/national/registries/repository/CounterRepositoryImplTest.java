package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.entity.CounterModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    @Mock
    private NationalRegistriesConfig nationalRegistriesConfig;

    private CounterRepositoryImpl counterRepository;

    @BeforeEach
    void setUp() {
        NationalRegistriesConfig.Anpr anpr = new NationalRegistriesConfig.Anpr();
        anpr.setTable("counter");
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(table);
        counterRepository = new CounterRepositoryImpl(nationalRegistriesConfig, dynamoDbEnhancedAsyncClient);
    }

    @Test
    void getCounter() {
        when(table.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(new CounterModel()));
        StepVerifier.create(counterRepository.getCounter(""))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void createUpdateItemEnhancedRequest() {
        UpdateItemEnhancedRequest<CounterModel> upd = counterRepository.createUpdateItemEnhancedRequest("anpr");
        assertNotNull(upd);
    }
}
