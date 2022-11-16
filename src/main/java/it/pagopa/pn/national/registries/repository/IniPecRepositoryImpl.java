package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IniPecRepositoryImpl implements IniPecRepository{

    private final DynamoDbAsyncTable<BatchRequest> tableBatch;
    private final DynamoDbAsyncTable<BatchPolling> tablePolling;

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;

    public IniPecRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient){
        this.tableBatch = dynamoDbEnhancedAsyncClient.table("pn-batchRequests", TableSchema.fromClass(BatchRequest.class));
        this.tablePolling = dynamoDbEnhancedAsyncClient.table("pn-BatchPolling", TableSchema.fromClass(BatchPolling.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedAsyncClient;
    }

    @Override
    public Mono<BatchRequest> saveRequestCF(GetDigitalAddressIniPECRequestBodyDto requestCf) {

        String cf = requestCf.getFilter().getTaxId();

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId(UUID.randomUUID().toString());
        batchRequest.setCf(cf);
        batchRequest.setRetry("0");
        batchRequest.setStatus("not worked");
        batchRequest.setLastReserved(LocalDateTime.now());

        return Mono.fromFuture(tableBatch.putItem(batchRequest)).thenReturn(batchRequest);
    }

    @Override
    public Mono<List<BatchRequest>> processingRecords(){
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.plusHours(1);
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#status","status");
        expressionNames.put("#batchId","batchId");
        expressionNames.put("#lastReserved","lastReserved");
        expressionValues.put(":status",AttributeValue.builder().s("not worked").build());
        expressionValues.put(":empty",AttributeValue.builder().s("").build());
        expressionValues.put(":lastReserved",AttributeValue.builder().s(localDateTime.toString()).build());

        String firstExpression = "#status = :status AND (attribute_not_exists(#batchId) OR #batchId = :empty) AND #lastReserved <= :lastReserved";

        Expression expression = Expression.builder()
                .expression(firstExpression)
                .expressionValues(expressionValues)
                .expressionNames(expressionNames)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .limit(10)
                .build();

        return Mono.from(tableBatch.scan(scanEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<Void> aggregateIdBatch(List<BatchRequest> batchRequests) {
        String idBatch = UUID.randomUUID().toString();

        batchRequests.forEach(batchRequest -> {
            batchRequest.setBatchId(idBatch);
            int retry = Integer.parseInt(batchRequest.getRetry()) + 1;
            batchRequest.setRetry(retry+"");
            batchRequest.setStatus("worked");
            batchRequest.setLastReserved(LocalDateTime.now());
            tableBatch.updateItem(batchRequest);
        });

        return Mono.empty();
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId){

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionNames.put("#batchId","batchId");
        expressionValues.put(":batchId",AttributeValue.builder().s(batchId).build());

        Expression expression = Expression.builder()
                .expression("#batchId = :batchId")
                .expressionValues(expressionValues)
                .expressionNames(expressionNames)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .limit(10)
                .build();

        return Mono.from(tableBatch.scan(scanEnhancedRequest)).map(Page::items);
    }

    @Override
    public Mono<BatchPolling> callIniPecAndAggregateCorrelationId(List<BatchRequest> batchRequests, String batchId){

        List<String> cfs = batchRequests.stream()
                .map(BatchRequest::getCorrelationId)
                .collect(Collectors.toList());

        //chiamo inipec passando i cfs e ritorna u correlationId che per noi è pollingId
        String pollidId = UUID.randomUUID().toString();

        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId(batchId);
        batchPolling.setPollingId(pollidId);
        batchPolling.setStatus("not worked");

        return Mono.fromFuture(tablePolling.putItem(batchPolling)).thenReturn(batchPolling);
    }

    public Mono<List<BatchPolling>> getBatchPollingByPollingId(String pollingId){
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        QueryConditional queryConditional = QueryConditional.sortBeginsWith(Key.builder()
                        .partitionValue(pollingId)
                        .build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                        .queryConditional(queryConditional)
                                .build();

        return Mono.from(tablePolling.query(queryEnhancedRequest)).map(Page::items);
    }

    public void triggeredPolling(BatchPolling batchPolling){

        //chiamiamo inipec con il loro correlationId


    }

}
