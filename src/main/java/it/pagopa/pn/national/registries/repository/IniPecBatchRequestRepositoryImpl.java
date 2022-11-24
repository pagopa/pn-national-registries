package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class IniPecBatchRequestRepositoryImpl implements IniPecBatchRequestRepository {

    private final DynamoDbAsyncTable<BatchRequest> tableBatch;

    public IniPecBatchRequestRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.tableBatch = dynamoDbEnhancedAsyncClient.table("pn-batchRequests", TableSchema.fromClass(BatchRequest.class));
    }
    @Override
    public Mono<BatchRequest> createBatchRequestByCf(GetDigitalAddressIniPECRequestBodyDto requestCf) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCf(requestCf.getFilter().getTaxId());
        return createBatchRequest(batchRequest);
    }
    private BatchRequest createNewStartBatchRequest(){
        BatchRequest batchRequest = new BatchRequest();
        //TO-DO correlation id lo genera dynamo
        batchRequest.setCorrelationId(UUID.randomUUID().toString());
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);

        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        return batchRequest;
    }
    private Mono<BatchRequest> createBatchRequest(BatchRequest batchRequest){
        return Mono.fromFuture(tableBatch.putItem(batchRequest)).thenReturn(batchRequest)
                .doOnNext(br -> log.info("Created Batch Request for taxId: {}",batchRequest.getCf()))
                .doOnError(throwable -> log.error("Failed to create Batch Request for taxId: {}",batchRequest.getCf()));
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestByNotBatchIdPageable(Map<String, AttributeValue> lastKey){

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue())))
                .limit(100);

        if(lastKey.size()!=0)
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(tableBatch.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestsByBatchIdAndSetStatus(String batchId, String status){
        return getBatchRequestByBatchId(batchId)
                .flatMap(batchRequests -> setStatusToBatchRequests(batchRequests,status));
    }
    private Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId){
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#status","status");
        expressionValues.put(":status",AttributeValue.builder().s(BatchStatus.WORKING.getValue()).build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder("#status = :status",expressionValues,expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(batchId)))
                .build();

        return Mono.from(tableBatch.index(BatchRequestConstant.GSI_BL).query(queryEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<List<BatchRequest>> setStatusToBatchRequests(List<BatchRequest> batchRequests, String status) {
        return Flux.fromIterable(batchRequests)
                .flatMap(batchRequest -> {
                    batchRequest.setStatus(status);
                    return Mono.fromFuture(tableBatch.updateItem(batchRequest));
                })
                .collectList();
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
                    else{
                        batchRequest.setBatchId(idBatch);
                    }
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
        return getBatchRequestToRecovery().flatMap(batchRequests -> resetBatchIdToBatchRequests(batchRequests));
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
