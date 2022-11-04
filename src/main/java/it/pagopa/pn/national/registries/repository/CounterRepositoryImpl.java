package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Component
@Slf4j
public class CounterRepositoryImpl implements CounterRepository{

    private final DynamoDbAsyncTable<CounterModel> table;

    public CounterRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.table = dynamoDbEnhancedAsyncClient.table("pn-counter", TableSchema.fromClass(CounterModel.class));
    }

    @Override
    public Mono<CounterModel> getCounter(String eservice) {
        long startTime = System.currentTimeMillis();
        return Mono.fromFuture(table.updateItem(createUpdateItemEnhancedRequest(eservice)).thenApply(counterModel -> {
            log.info("updateItem timelapse: {}ms",System.currentTimeMillis()-startTime);
            return counterModel;
        }));
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
