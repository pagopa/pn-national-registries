package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Component
@Slf4j
public class CounterRepositoryImpl implements CounterRepository {

    private final DynamoDbAsyncTable<CounterModel> table;

    public CounterRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 @Value("${pn.national.registries.pdnd.anpr.table}") String tableName) {
        this.table = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromClass(CounterModel.class));
    }

    @Override
    public Mono<CounterModel> getCounter(String eService) {
        return Mono.fromFuture(table.updateItem(createUpdateItemEnhancedRequest(eService)))
                .map(counterModel -> counterModel);
    }

    protected UpdateItemEnhancedRequest<CounterModel> createUpdateItemEnhancedRequest(String eService) {
        CounterModel counterModel = new CounterModel();
        counterModel.setEservice(eService);
        return UpdateItemEnhancedRequest
                .builder(CounterModel.class)
                .item(counterModel)
                .build();
    }
}
