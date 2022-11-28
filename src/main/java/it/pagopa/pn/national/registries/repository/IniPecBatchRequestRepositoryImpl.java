package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IniPecBatchRequestRepositoryImpl implements IniPecBatchRequestRepository {

    private final DynamoDbAsyncTable<BatchRequest> tableBatch;

    public IniPecBatchRequestRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.tableBatch = dynamoDbEnhancedAsyncClient.table("pn-batchRequests", TableSchema.fromClass(BatchRequest.class));
    }

    @Override
    public Mono<BatchRequest> createBatchRequest(BatchRequest batchRequest){
        return Mono.fromFuture(tableBatch.putItem(batchRequest)).thenReturn(batchRequest);
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestByNotBatchIdPageable(Map<String, AttributeValue> lastKey){
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#status","status");
        expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder("#status = :status",expressionValues,expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue())))
                .limit(100);

        if(lastKey.size()!=0)
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(tableBatch.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestsToSend(String batchId) {
        return getBatchRequestByBatchId(batchId);
    }

    @Override
    public Mono<BatchRequest> setBatchRequestsStatus(BatchRequest batchRequest, String status){
        batchRequest.setStatus(status);
        return Mono.fromFuture(tableBatch.updateItem(batchRequest));
    }

    private Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId){
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(batchId)))
                .build();

        return Mono.from(tableBatch.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<List<BatchRequest>> setNewBatchIdToBatchRequests(List<BatchRequest> batchRequests, String idBatch) {
        return Flux.fromIterable(batchRequests)
                .flatMap(batchRequest -> {
                    batchRequest.setRetry(batchRequest.getRetry() + 1);
                    batchRequest.setStatus(BatchStatus.WORKING.getValue());
                    if (batchRequest.getRetry() > 3) {
                        batchRequest.setStatus(BatchStatus.ERROR.getValue());
                    }
                    batchRequest.setBatchId(idBatch);
                    batchRequest.setLastReserved(LocalDateTime.now());

                    Map<String, AttributeValue> expressionValues = new HashMap<>();
                    Map<String, String> expressionNames = new HashMap<>();

                    expressionNames.put("#batchId","batchId");
                    expressionNames.put("#status","status");

                    expressionValues.put(":batchId",AttributeValue.builder().s(BatchStatus.NO_BATCH_ID.getValue()).build());
                    expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

                    UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                            .item(batchRequest)
                            .conditionExpression(expressionBuilder("#batchId = :batchId AND #status = :status",expressionValues,expressionNames))
                            .build();
                    return Mono.fromFuture(tableBatch.updateItem(updateItemEnhancedRequest));
                })
                .collectList()
                .flatMap(batchRequests1 -> getBatchRequestByBatchId(idBatch));
    }

    @Override
    public Mono<List<BatchRequest>> resetBatchIdForRecovery(){
        return getBatchRequestToRecovery().flatMap(this::resetBatchIdToBatchRequests);
    }

    private Mono<List<BatchRequest>> getBatchRequestToRecovery(){
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#retry","retry");
        expressionNames.put("#lastReserved","lastReserved");

        expressionValues.put(":retry",AttributeValue.builder().n("3").build());
        expressionValues.put(":lastReserved",AttributeValue.builder().s(LocalDateTime.now().minusHours(1).toString()).build());

        String expression = "#retry <= :retry AND :lastReserved > #lastReserved";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.WORKING.getValue()));

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression,expressionValues,expressionNames));

        return Mono.from(tableBatch.index(BatchRequestConstant.GSI_S).query(queryEnhancedRequestBuilder.build())).map(Page::items);
    }

    @Override
    public Mono<List<BatchRequest>> resetBatchIdToBatchRequests(List<BatchRequest> batchRequests) {
        return Flux.fromIterable(batchRequests)
                .flatMap(batchRequest -> {
                    batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                    batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
                    return Mono.fromFuture(tableBatch.updateItem(batchRequest));
                })
                .collectList();
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
