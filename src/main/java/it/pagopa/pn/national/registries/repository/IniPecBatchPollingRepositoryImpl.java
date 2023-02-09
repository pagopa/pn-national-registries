package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.national.registries.constant.BatchPollingConstant.*;

@Component
public class IniPecBatchPollingRepositoryImpl implements IniPecBatchPollingRepository {

    private final DynamoDbAsyncTable<BatchPolling> table;

    private final int maxRetry;

    public IniPecBatchPollingRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                            @Value("${pn.national-registries.inipec.batch.polling.max-retry}") int maxRetry) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-batchPolling", TableSchema.fromClass(BatchPolling.class));
        this.maxRetry = maxRetry;
    }

    @Override
    public Mono<BatchPolling> create(BatchPolling batchPolling) {
        return Mono.fromFuture(table.putItem(batchPolling)).thenReturn(batchPolling);
    }

    @Override
    public Mono<BatchPolling> update(BatchPolling batchPolling) {
        return Mono.fromFuture(table.updateItem(batchPolling));
    }

    @Override
    public Mono<Page<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWorked(Map<String, AttributeValue> lastKey, int limit) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NOT_WORKED.getValue()));

        String expression = "attribute_not_exists(#reservationId) OR size(#reservationId) = :zero";
        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_S).query(queryEnhancedRequest));
    }

    @Override
    public Mono<BatchPolling> setNewReservationIdToBatchPolling(BatchPolling batchPolling) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);
        expressionNames.put("#status", COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":status", AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        String expression = "(attribute_not_exists(#reservationId) OR size(#reservationId) = :zero) AND #status = :status";
        UpdateItemEnhancedRequest<BatchPolling> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchPolling.class)
                .item(batchPolling)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<BatchPolling>> getBatchPollingToRecover() {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#retry", COL_RETRY);
        expressionNames.put("#lastReserved", COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(maxRetry)).build());
        expressionValues.put(":lastReserved", AttributeValue.builder().s(LocalDateTime.now(ZoneOffset.UTC).minusHours(1).toString()).build());

        String expression = "#retry < :retry AND :lastReserved > #lastReserved";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.WORKING.getValue()));

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Flux.from(table.index(GSI_S).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
    }

    private Key keyBuilder(String key) {
        return Key.builder().partitionValue(key).build();
    }

    private Expression expressionBuilder(String expression, Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames) {
        Expression.Builder expressionBuilder = Expression.builder();
        if (expression != null) {
            expressionBuilder.expression(expression);
        }
        if (expressionValues != null) {
            expressionBuilder.expressionValues(expressionValues);
        }
        if (expressionNames != null) {
            expressionBuilder.expressionNames(expressionNames);
        }
        return expressionBuilder.build();
    }
}
