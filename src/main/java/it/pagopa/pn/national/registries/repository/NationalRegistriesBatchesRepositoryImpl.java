package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@CustomLog
public class NationalRegistriesBatchesRepositoryImpl implements NationalRegistriesBatchesRepository {

    private static final String STATUS_ATTEMPT_AFTER_INDEX = "StatusAttemptAfterIndex";
    private static final String SORT_KEY_VALUE = "-";
    private static final String STATUS_ALIAS = "#status";
    private static final String EXPECTED_STATUS_PLACEHOLDER = ":expectedStatus";
    private static final String TECHNICAL_ERROR = "tecnico";
    private static final String IN_PROGRESS_ERROR = "inProgress";

    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final DynamoDbAsyncTable<NationalRegistriesBatch> table;

    public NationalRegistriesBatchesRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                                   NationalRegistriesConfig nationalRegistriesConfig) {
        this.table = dynamoDbEnhancedAsyncClient.table(nationalRegistriesConfig.getDao().getNationalRegistriesBatchesTableName(), TableSchema.fromClass(NationalRegistriesBatch.class));
        this.nationalRegistriesConfig = nationalRegistriesConfig;
    }

    @Override
    public Mono<NationalRegistriesBatch> putBatch(NationalRegistriesBatch batch) {
        log.debug("Inserting data {} in DynamoDB table {}", batch, table);
        return Mono.fromFuture(table.putItem(batch))
                .doOnNext(unused -> log.info("Inserted data in DynamoDB table {}", table))
                .thenReturn(batch);
    }

    @Override
    public Mono<NationalRegistriesBatch> getBatch(String batchId) {
        return Mono.fromFuture(table.getItem(Key.builder()
                .partitionValue(batchId)
                .sortValue(SORT_KEY_VALUE)
                .build()));
    }

    @Override
    public Mono<Page<NationalRegistriesBatch>> getBatchesToProcess(String status, Instant now, Map<String, AttributeValue> lastEvaluatedKey) {
        Key key = Key.builder()
                .partitionValue(status)
                .sortValue(now.toString())
                .build();

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.sortLessThanOrEqualTo(key))
                .limit(nationalRegistriesConfig.getQueryLimit());

        if (!CollectionUtils.isEmpty(lastEvaluatedKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
        }

        return Mono.from(table.index(STATUS_ATTEMPT_AFTER_INDEX)
                .query(queryEnhancedRequestBuilder.build()));
    }


    @Override
    public Mono<NationalRegistriesBatch> updateStatusAndAttemptAfter(NationalRegistriesBatch batch, String fromStatus, String toStatus, String errorType, Boolean isError) {
        if(isError){
            batch.setAttemptAfter(calculateAttemptAfter(errorType));
        }

        batch.setStatus(toStatus);
        batch.setUpdatedAt(Instant.now());

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(STATUS_ALIAS, "status");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(EXPECTED_STATUS_PLACEHOLDER, AttributeValue.builder().s(fromStatus).build());

        Expression conditionExpression = Expression.builder()
                .expression(STATUS_ALIAS + " = " + EXPECTED_STATUS_PLACEHOLDER)
                .expressionNames(expressionNames)
                .expressionValues(expressionValues)
                .build();

        UpdateItemEnhancedRequest<NationalRegistriesBatch> updateItemEnhancedRequest = UpdateItemEnhancedRequest
                .builder(NationalRegistriesBatch.class)
                .item(batch)
                .conditionExpression(conditionExpression)
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest))
                .doOnError(error -> log.warn("Unable to update batch {} after {} error", batch.getBatchId(), errorType, error));

    }

    private Instant calculateAttemptAfter(String errorType) {
        NationalRegistriesConfig.Inad inadConfig = nationalRegistriesConfig.getInad();

        int retryAfter = switch (errorType) {
            case TECHNICAL_ERROR -> inadConfig.getPollingRetryAfter();
            case IN_PROGRESS_ERROR -> inadConfig.getPollingInProgressRetryAfter();
            default -> throw new IllegalArgumentException("Unsupported batch error type: " + errorType);
        };

        return Instant.now().plusSeconds(retryAfter);
    }
}
