package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
@CustomLog
public class NationalRegistriesBatchesRepositoryImpl implements NationalRegistriesBatchesRepository {

    private static final String SORT_KEY_VALUE = "-";
    private static final String STATUS_ALIAS = "#status";
    private static final String EXPECTED_STATUS_PLACEHOLDER = ":expectedStatus";

    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final DynamoDbAsyncTable<NationalRegistriesBatch> table;

    public NationalRegistriesBatchesRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                                   NationalRegistriesConfig nationalRegistriesConfig) {
        this.table = dynamoDbEnhancedAsyncClient.table(nationalRegistriesConfig.getDao().getBatchesTableName(), TableSchema.fromClass(NationalRegistriesBatch.class));
        this.nationalRegistriesConfig = nationalRegistriesConfig;
    }

    @Override
    public Mono<NationalRegistriesBatch> putBatch(NationalRegistriesBatch batch) {
        log.debug("Inserting batch with batchId: {} ", batch.getBatchId());
        return Mono.fromFuture(table.putItem(batch))
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
    public Mono<Page<NationalRegistriesBatch>> getBatchesToProcess(BatchesStatusEnum status, Instant now, Map<String, AttributeValue> lastEvaluatedKey) {
        Key key = Key.builder().partitionValue(status.name()).sortValue(now.toString()).build();

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.sortLessThanOrEqualTo(key))
                .limit(nationalRegistriesConfig.getQueryLimit());

        if (!CollectionUtils.isEmpty(lastEvaluatedKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
        }

        return Mono.from(table.index(NationalRegistriesBatch.STATUS_ATTEMPTAFTER_INDEX)
                .query(queryEnhancedRequestBuilder.build()));
    }

    @Override
    public Mono<NationalRegistriesBatch> updateItem(NationalRegistriesBatch batch, BatchesStatusEnum fromStatus) {
        Instant now = Instant.now();
        batch.setUpdatedAt(now);

        UpdateItemEnhancedRequest.Builder<NationalRegistriesBatch> updateRequest = UpdateItemEnhancedRequest.builder(NationalRegistriesBatch.class)
                .item(batch);

        if(Objects.nonNull(fromStatus)) {
            Map<String, String> expressionNames = Map.of(STATUS_ALIAS, NationalRegistriesBatch.COL_STATUS);
            Map<String, AttributeValue> expressionValues = Map.of(EXPECTED_STATUS_PLACEHOLDER, AttributeValue.builder().s(fromStatus.name()).build());
            Expression conditionExpression = Expression.builder()
                    .expression(STATUS_ALIAS + " = " + EXPECTED_STATUS_PLACEHOLDER)
                    .expressionNames(expressionNames)
                    .expressionValues(expressionValues)
                    .build();
            updateRequest.conditionExpression(conditionExpression);
        }

        return Mono.fromFuture(table.updateItem(updateRequest.build()))
                .doOnSuccess(updated -> log.info(
                        "Updated batch {} from status {} to status {}, registry {}, attemptAfter {}",
                        batch.getBatchId(),
                        fromStatus,
                        batch.getStatus(),
                        batch.getRegistry(),
                        batch.getAttemptAfter()
                ))
                .doOnError(error -> log.warn(
                        "Unable to update batch {} from status {} to status {}, registry {}",
                        batch.getBatchId(),
                        fromStatus,
                        batch.getStatus(),
                        batch.getRegistry(),
                        error
                ));
    }


    private Instant calculateAttemptAfter(GatewayDownstreamService registry, boolean inProgressRetry, Instant now) {
        int retryAfter = switch (registry) {
            case INIPEC -> getInipecRetryAfter(inProgressRetry);
            case INAD -> getInadRetryAfter(inProgressRetry);
            default -> throw new IllegalArgumentException("Polling retry configuration not supported for registry: " + registry);
        };

        return now.plusSeconds(retryAfter);
    }

    private int getInipecRetryAfter(boolean inProgressRetry) {
        NationalRegistriesConfig.Inipec inipecConfig = nationalRegistriesConfig.getInipec();
        return inProgressRetry ? inipecConfig.getPollingInProgressRetryAfter() : inipecConfig.getPollingRetryAfter();
    }

    private int getInadRetryAfter(boolean inProgressRetry) {
        NationalRegistriesConfig.Inad inadConfig = nationalRegistriesConfig.getInad();
        return inProgressRetry ? inadConfig.getPollingInProgressRetryAfter() : inadConfig.getPollingRetryAfter();
    }
}
