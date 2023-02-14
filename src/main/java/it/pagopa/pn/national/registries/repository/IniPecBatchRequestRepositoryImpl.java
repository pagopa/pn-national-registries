package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.national.registries.constant.BatchRequestConstant.*;

@Component
public class IniPecBatchRequestRepositoryImpl implements IniPecBatchRequestRepository {

    private final DynamoDbAsyncTable<BatchRequest> table;

    private final int maxRetry;
    private final int retryAfter;

    private static final String STATUS_ALIAS = "#status";
    private static final String STATUS_PLACEHOLDER = ":status";
    private static final String STATUS_EQ = STATUS_ALIAS + " = " + STATUS_PLACEHOLDER;

    private static final String LAST_RESERVED_ALIAS = "#lastReserved";
    private static final String LAST_RESERVED_PLACEHOLDER = ":lastReserved";
    private static final String LAST_RESERVED_EQ = LAST_RESERVED_ALIAS + " = " + LAST_RESERVED_PLACEHOLDER;

    public IniPecBatchRequestRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                            @Value("${pn.national-registries.inipec.batch.request.max-retry}") int maxRetry,
                                            @Value("${pn.national-registries.inipec.batch.request.recovery.after}") int retryAfter) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-batchRequests", TableSchema.fromClass(BatchRequest.class));
        this.maxRetry = maxRetry;
        this.retryAfter = retryAfter;
    }

    @Override
    public Mono<BatchRequest> update(BatchRequest batchRequest) {
        return Mono.fromFuture(table.updateItem(batchRequest));
    }

    @Override
    public Mono<BatchRequest> create(BatchRequest batchRequest) {
        return Mono.fromFuture(table.putItem(batchRequest)).thenReturn(batchRequest);
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder(STATUS_EQ, expressionValues, expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue())))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestByBatchIdAndStatus(String batchId, BatchStatus status) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(status.getValue()).build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder(STATUS_EQ, expressionValues, expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(batchId)))
                .build();

        return Flux.from(table.index(GSI_BL).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
    }

    @Override
    public Mono<BatchRequest> setNewBatchIdToBatchRequest(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#batchId", COL_BATCH_ID);
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":batchId", AttributeValue.builder().s(BatchStatus.NO_BATCH_ID.getValue()).build());
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        String expression = "#batchId = :batchId AND " + STATUS_EQ;
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<BatchRequest> setNewReservationIdToBatchRequest(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);
        expressionNames.put("#sendStatus", COL_SEND_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":sendStatus", AttributeValue.builder().s(BatchStatus.NOT_SENT.getValue()).build());
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        String expression = "(attribute_not_exists(#reservationId) OR size(#reservationId) = :zero) AND #sendStatus = :sendStatus";
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<BatchRequest> resetBatchRequestForRecovery(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        AttributeValue lastReserved = AttributeValue.builder()
                .s(batchRequest.getLastReserved() != null ? batchRequest.getLastReserved().toString() : "")
                .build();
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, lastReserved);

        String expression = LAST_RESERVED_EQ + " OR attribute_not_exists(" + LAST_RESERVED_ALIAS + ")";
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestToRecovery() {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#retry", COL_RETRY);
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(maxRetry)).build());
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, AttributeValue.builder()
                .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(retryAfter).toString())
                .build());

        String expression = "#retry < :retry AND (:lastReserved > #lastReserved OR attribute_not_exists(#lastReserved))";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.WORKING.getValue()));

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Flux.from(table.index(GSI_S).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestToSend(Map<String, AttributeValue> lastKey, int limit) {
        Key key = Key.builder()
                .partitionValue(BatchStatus.NOT_SENT.getValue())
                .sortValue(AttributeValue.builder()
                        .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(retryAfter).toString())
                        .build())
                .build();

        QueryConditional queryConditional = QueryConditional.sortLessThan(key);

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_SSL).query(queryEnhancedRequest));
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
