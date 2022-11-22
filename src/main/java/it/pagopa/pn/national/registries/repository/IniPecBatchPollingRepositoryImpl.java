package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
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
    public Mono<List<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWork(){
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId","reservationId");
        expressionNames.put("#status","status");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());


        Expression expression = Expression.builder()
                .expression("attribute_not_exists(#reservationId) AND #status = :status")
                .expressionNames(expressionNames)
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();

        return Mono.from(tablePolling.scan(scanEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<List<BatchPolling>> setReservationIdToAndStatusToWorkBatchPolling(List<BatchPolling> batchPollings, String reservationId){
        return Flux.fromIterable(batchPollings)
                .flatMap(batchPolling -> {
                    batchPolling.setStatus(BatchStatus.TO_WORK.getValue());
                    batchPolling.setReservationId(reservationId);
                    return Mono.fromFuture(tablePolling.updateItem(batchPolling));
                })
                .collectList();
    }

    @Override
    public Mono<List<BatchPolling>> getBatchPollingByReservationIdAndStatusToWork(String reservationId){
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId","reservationId");
        expressionNames.put("#status","status");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":reservationId",AttributeValue.builder().s(reservationId).build());
        expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.TO_WORK.getValue()).build());


        Expression expression = Expression.builder()
                .expression("#reservationId = :reservationId AND #status = :status")
                .expressionValues(expressionValues)
                .expressionNames(expressionNames)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();
        return Mono.from(tablePolling.scan(scanEnhancedRequest)).map(Page::items);
    }

}
