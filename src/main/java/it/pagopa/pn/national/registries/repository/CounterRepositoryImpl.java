package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Component
@Slf4j
public class CounterRepositoryImpl implements CounterRepository{

    private final DynamoDbAsyncTable<CounterModel> table;

    public CounterRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 @Value("${pn.national.registries.pdnd.anpr.table}") String tableName){
        this.table = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromClass(CounterModel.class));
    }

    @Override
    public Mono<CounterModel> getCounter(String eservice) {
        return Mono.fromFuture(table.updateItem(createUpdateItemEnhancedRequest(eservice)))
                .map(counterModel -> counterModel);
    }

    protected UpdateItemEnhancedRequest<CounterModel> createUpdateItemEnhancedRequest(String eservice) {
        CounterModel counterModel = new CounterModel();
        counterModel.setEservice(eservice);
        return UpdateItemEnhancedRequest
                .builder(CounterModel.class)
                .item(counterModel)
                .build();
    }
}
