package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IniPecBatchPollingRepositoryImpl implements IniPecBatchPollingRepository{

    private final DynamoDbAsyncTable<BatchPolling> tablePolling;

    public IniPecBatchPollingRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.tablePolling = dynamoDbEnhancedAsyncClient.table("pn-BatchPolling", TableSchema.fromClass(BatchPolling.class));
    }

    @Override
    public Mono<BatchPolling> createBatchPolling(BatchPolling batchPolling){
        return Mono.fromFuture(tablePolling.putItem(batchPolling)).thenReturn(batchPolling);
    }
    @Override
    public Mono<BatchPolling> updateBatchPolling(BatchPolling batchPolling){
        return Mono.fromFuture(tablePolling.updateItem(batchPolling)).thenReturn(batchPolling);
    }

    @Override
    public Mono<Page<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWork(Map<String, AttributeValue> lastKey){
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#reservationId","reservationId");

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NOT_WORKED.getValue()));

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder("attribute_not_exists(#reservationId)",null,expressionNames))
                .limit(1);

        if(lastKey.size()!=0)
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);

        return Mono.from(tablePolling.index(BatchRequestConstant.GSI_S).query(queryEnhancedRequestBuilder.build()));
    }

    @Override
    public Mono<List<BatchPolling>> setReservationIdToAndStatusWorkingBatchPolling(List<BatchPolling> batchPollings, String reservationId){
        return Flux.fromIterable(batchPollings)
                .flatMap(batchPolling -> {
                    batchPolling.setStatus(BatchStatus.WORKING.getValue());
                    batchPolling.setReservationId(reservationId);
                    return Mono.fromFuture(tablePolling.updateItem(batchPolling));
                })
                .collectList()
                .flatMap(batchPollingWithReservationId -> getBatchPollingByReservationIdAndStatusWorking(reservationId));
    }

    @Override
    public Mono<List<BatchPolling>> getBatchPollingByReservationIdAndStatusWorking(String reservationId){
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId","reservationId");
        expressionNames.put("#status","status");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":reservationId",AttributeValue.builder().s(reservationId).build());
        expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.WORKING.getValue()).build());

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(expressionBuilder("#reservationId = :reservationId AND #status = :status",expressionValues,expressionNames))
                .build();
        return Mono.from(tablePolling.scan(scanEnhancedRequest)).map(Page::items);
    }

    private Key keyBuilder(String key){
        return Key.builder().partitionValue(key).build();
    }

    private Expression expressionBuilder(String expression, Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames){
        Expression.Builder expressionBuilder = Expression.builder();
        if(expression!=null)
            expressionBuilder.expression(expression);
        if(expressionValues!=null)
            expressionBuilder.expressionValues(expressionValues);
        if(expressionNames!=null)
            expressionBuilder.expressionNames(expressionNames);
        return expressionBuilder.build();
    }
}
