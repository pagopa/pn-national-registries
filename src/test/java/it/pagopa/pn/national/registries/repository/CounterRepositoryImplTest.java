package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CounterRepositoryImpl.class)
class CounterRepositoryImplTest {

    @Autowired
    private CounterRepositoryImpl counterRepository;

    @MockBean
    private DynamoDbAsyncTable<CounterModel> table;

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Test
    void createUpdateItemEnhancedRequest() {
        UpdateItemEnhancedRequest<CounterModel> upd = counterRepository.createUpdateItemEnhancedRequest("anpr");
        Assertions.assertNotNull(upd);
    }
}
