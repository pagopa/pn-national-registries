package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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

import static it.pagopa.pn.national.registries.constant.BatchRequestConstant.COL_BATCH_ID;
import static it.pagopa.pn.national.registries.constant.BatchRequestConstant.COL_STATUS;

@Component
public class IniPecBatchRequestRepositoryImpl implements IniPecBatchRequestRepository {

    private final DynamoDbAsyncTable<BatchRequest> table;

    private final int maxRetry;

    public IniPecBatchRequestRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                            @Value("${pn.national-registries.inipec.batch.request.max-retry}") int maxRetry) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-batchRequests", TableSchema.fromClass(BatchRequest.class));
        this.maxRetry = maxRetry;
    }

    @Override
    public Mono<BatchRequest> update(BatchRequest batchRequest) {
        return Mono.fromFuture(table.updateItem(batchRequest));
    }

    @Override
    public Mono<BatchRequest> createBatchRequest(BatchRequest batchRequest) {
        return Mono.fromFuture(table.putItem(batchRequest)).thenReturn(batchRequest);
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#status", COL_STATUS);
        expressionValues.put(":status", AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder("#status = :status", expressionValues, expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue())))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestsToSend(String batchId) {
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(batchId)))
                .build();

        return Mono.from(table.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<BatchRequest> setBatchRequestsStatus(BatchRequest batchRequest, String status) {
        batchRequest.setStatus(status);
        return Mono.fromFuture(table.updateItem(batchRequest));
    }

    @Override
    public Mono<BatchRequest> setNewBatchIdToBatchRequest(BatchRequest batchRequest) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#batchId", COL_BATCH_ID);
        expressionNames.put("#status", COL_STATUS);

        expressionValues.put(":batchId", AttributeValue.builder().s(BatchStatus.NO_BATCH_ID.getValue()).build());
        expressionValues.put(":status", AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder("#batchId = :batchId AND #status = :status", expressionValues, expressionNames))
                .build();
        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestToRecovery() {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#retry", BatchRequestConstant.COL_RETRY);
        expressionNames.put("#lastReserved", BatchRequestConstant.COL_LAST_RESERVED);

        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(maxRetry)).build());
        expressionValues.put(":lastReserved", AttributeValue.builder().s(LocalDateTime.now(ZoneOffset.UTC).minusHours(1).toString()).build());

        String expression = "#retry < :retry AND :lastReserved > #lastReserved";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.WORKING.getValue()));

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.from(table.index(BatchRequestConstant.GSI_S).query(queryEnhancedRequest)).map(Page::items);
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
