package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Component
public class CounterRepositoryImpl implements CounterRepository{

    private final DynamoDbAsyncTable<CounterModel> table;

    public CounterRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.table = dynamoDbEnhancedAsyncClient.table("pn-counter", TableSchema.fromClass(CounterModel.class));
    }

    @Override
    public Mono<CounterModel> getCounter(String eservice) {
        return Mono.fromFuture(table.updateItem(createUpdateItemEnhancedRequest(eservice)).thenApply(counterModel -> counterModel))
                .flatMap(s -> Mono.fromFuture(table.getItem(Key.builder().partitionValue(eservice).build())
                        .thenApply(counterModel -> counterModel)));
    }

    private UpdateItemEnhancedRequest<CounterModel> createUpdateItemEnhancedRequest(String eservice) {
        CounterModel counterModel = new CounterModel();
        counterModel.setEservice(eservice);
        return UpdateItemEnhancedRequest
                .builder(CounterModel.class)
                .item(counterModel)
                .build();
    }
}
