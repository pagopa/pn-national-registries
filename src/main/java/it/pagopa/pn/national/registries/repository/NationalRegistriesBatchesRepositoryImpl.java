package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.national.registries.constant.BatchesStatusEnum.REQUEST_SENT;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;

@Component
@CustomLog
public class NationalRegistriesBatchesRepositoryImpl implements NationalRegistriesBatchesRepository {

    private static final String SORT_KEY_VALUE = "-";

    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final DynamoDbAsyncTable<NationalRegistriesBatch> table;
    private final DynamoDbClient dynamoDbClient;


    public NationalRegistriesBatchesRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                                   NationalRegistriesConfig nationalRegistriesConfig, DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
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
        Key key = Key.builder().partitionValue(status.name()).sortValue(formatInstant(now)).build();

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
    public Mono<NationalRegistriesBatch> updateItem(NationalRegistriesBatch batch, BatchesStatusEnum newStatus) {
        Instant now = Instant.now();
        BatchesStatusEnum expectedStatus = retrieveExpectedStatus(newStatus);

        batch.setStatus(newStatus);
        batch.setUpdatedAt(now);

        Expression conditionExpression = Expression.builder()
                .expression("#status = :expectedStatus")
                .expressionNames(Map.of("#status", NationalRegistriesBatch.COL_STATUS))
                .expressionValues(Map.of(":expectedStatus", AttributeValue.builder().s(expectedStatus.name()).build()))
                .build();

        UpdateItemEnhancedRequest<NationalRegistriesBatch> updateRequest = UpdateItemEnhancedRequest.builder(NationalRegistriesBatch.class)
                .conditionExpression(conditionExpression)
                .item(batch)
                .build();

        return Mono.fromFuture(table.updateItem(updateRequest))
                .doOnSuccess(updated -> log.info("Updated batch {} from status {} to status {}, registry {}, attemptAfter {}", batch.getBatchId(), expectedStatus, newStatus, batch.getRegistry(), batch.getAttemptAfter()))
                .doOnError(error -> log.warn("Unable to update batch {} from status {} to status {}, registry {}", batch.getBatchId(), expectedStatus, newStatus, batch.getRegistry(), error));
    }


    /**
     * Aggiorna lo stato della richiesta a ERROR o WORKED.
     * La transizione è consentita solo da WORKING.
     * ERROR e WORKED sono stati finali, quindi viene aggiornato anche il TTL.
     */
    @Override
    public Mono<NationalRegistriesBatch> updateToFinalStatus(String pk, String sk, BatchesStatusEnum finalStatus) {
        Instant now = Instant.now();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        addTtl(names, values, updates, now);

        return updateStatus(pk, sk, finalStatus, names, values, updates, now);
    }

    /**
     * Reimposta lo stato della richiesta a REQUEST_SENT
     * per consentire un nuovo tentativo di elaborazione.
     * La transizione consentita è:
     * WORKING -> REQUEST_SENT
     * Contestualmente aggiorna il contatore dei retry (retry standard o in progress retry) e l'attributo attemptAfter.
     */
    @Override
    public Mono<NationalRegistriesBatch> resetStatusForRetry(String pk, String sk, Integer retry, boolean isInProgressRetry, GatewayDownstreamService registry) {
        Instant now = Instant.now();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        Instant attemptAfter = calculateAttemptAfter(registry, isInProgressRetry, now);

        if (isInProgressRetry) {
            names.put("#inProgressRetryCount", NationalRegistriesBatch.COL_INPROGRESSRETRYCOUNT);
            values.put(":inProgressRetryCount", AttributeValue.builder().n(String.valueOf(retry)).build());
            updates.add("#inProgressRetryCount = :inProgressRetryCount");
        } else {
            names.put("#retryCount", NationalRegistriesBatch.COL_ERRORRETRYCOUNT);
            values.put(":retryCount", AttributeValue.builder().n(String.valueOf(retry)).build());
            updates.add("#retryCount = :retryCount");
        }

        names.put("#attemptAfter", NationalRegistriesBatch.COL_ATTEMPTAFTER);
        values.put(":attemptAfter", AttributeValue.builder().s(formatInstant(attemptAfter)).build());
        updates.add("#attemptAfter = :attemptAfter");

        return updateStatus(pk, sk, REQUEST_SENT, names, values, updates, now);
    }


    public Mono<NationalRegistriesBatch> updateStatus(String pk, String sk, BatchesStatusEnum newStatus, Map<String, String> names, Map<String, AttributeValue> values, List<String> updates, Instant now) {

        BatchesStatusEnum expectedStatus = retrieveExpectedStatus(newStatus);
        names.put("#status", NationalRegistriesRequest.COL_STATUS);
        values.put(":expectedStatus", AttributeValue.builder().s(expectedStatus.name()).build());
        values.put(":newStatus", AttributeValue.builder().s(newStatus.name()).build());
        updates.add("#status = :newStatus");

        names.put("#updatedAt", NationalRegistriesBatch.COL_UPDATEDAT);
        values.put(":updatedAt", AttributeValue.builder().s(now.toString()).build());
        updates.add("#updatedAt = :updatedAt");

        UpdateItemRequest request = UpdateItemRequest.builder().tableName(nationalRegistriesConfig.getDao().getBatchesTableName())
                .key(Map.of(NationalRegistriesBatch.COL_BATCHID, AttributeValue.builder().s(pk).build(),
                        NationalRegistriesBatch.COL_SK, AttributeValue.builder().s(sk).build()))
                .updateExpression("SET " + String.join(", ", updates))
                .conditionExpression("#status = :expectedStatus")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

            return Mono.just(dynamoDbClient.updateItem(request))
                    .map(updateItemResponse -> NationalRegistriesBatch.attributeValueMapToNationalRegistriesRequest(updateItemResponse.attributes()))
                    .doOnSuccess(updated -> log.info("Updated batch {} from status {} to status {}", pk, expectedStatus, newStatus))
                    .doOnError(error -> log.warn("Unable to update batch {} from status {} to status {}", pk, expectedStatus, newStatus, error));
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
        return inProgressRetry ? inipecConfig.getPollingInProgressRetryAfter() : inipecConfig.getPollingErrorRetryAfter();
    }

    private int getInadRetryAfter(boolean inProgressRetry) {
        NationalRegistriesConfig.Inad inadConfig = nationalRegistriesConfig.getInad();
        return inProgressRetry ? inadConfig.getPollingInProgressRetryAfter() : inadConfig.getPollingErrorRetryAfter();
    }

    private BatchesStatusEnum retrieveExpectedStatus(BatchesStatusEnum newStatus) {
        return switch (newStatus) {
            case WORKING -> REQUEST_SENT;
            case WORKED, ERROR, REQUEST_SENT -> BatchesStatusEnum.WORKING;
        };
    }

    private void addTtl(Map<String, String> names, Map<String, AttributeValue> values, List<String> updates, Instant now) {
        long ttl = now.plusSeconds(nationalRegistriesConfig.getTtl()).getEpochSecond();
        names.put("#ttl", NationalRegistriesRequest.COL_TTL);
        values.put(":ttl", AttributeValue.builder().n(Long.toString(ttl)).build());
        updates.add("#ttl = :ttl");
    }
}
