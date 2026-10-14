package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.national.registries.constant.RequestStatusEnum.ERROR;
import static it.pagopa.pn.national.registries.constant.RequestStatusEnum.NOT_WORKED;
import static it.pagopa.pn.national.registries.constant.RequestStatusEnum.WORKED;
import static it.pagopa.pn.national.registries.constant.RequestStatusEnum.WORKING;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_CONDITIONALCHECKFAILED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_REQUIRED_BATCHID;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_REQUIRED_RESULTPAYLOAD;

@Component
@Slf4j
public class NationalRegistriesRequestsRepositoryImpl implements NationalRegistriesRequestsRepository {

    private final DynamoDbAsyncTable<NationalRegistriesRequest> table;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final DynamoDbClient dynamoDbClient;

    public NationalRegistriesRequestsRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, NationalRegistriesConfig nationalRegistriesConfig, DynamoDbClient dynamoDbClient) {
        this.nationalRegistriesConfig = nationalRegistriesConfig;
        this.dynamoDbClient = dynamoDbClient;
        this.table = dynamoDbEnhancedAsyncClient.table(nationalRegistriesConfig.getDao().getRequestsTableName(), TableSchema.fromClass(NationalRegistriesRequest.class));
    }

    @Override
    public Mono<NationalRegistriesRequest> putRequest(NationalRegistriesRequest nationalRegistriesRequest) {
        return Mono.fromFuture(() -> table.putItem(nationalRegistriesRequest))
                .thenReturn(nationalRegistriesRequest);
    }

    /**
     * Recupera le richieste, paginate, associate a uno specifico batchId per l'elaborazione nella fase di polling
     *
     */
    @Override
    public Mono<Page<NationalRegistriesRequest>> getRequestsBatchId(String batchId, RequestStatusEnum status, Map<String, AttributeValue> lastEvaluatedKey) {
        Key key;
        if(Objects.isNull(status)) {
            key = Key.builder().partitionValue(batchId).build();
        }else{
            key = Key.builder().partitionValue(batchId).sortValue(status.name()).build();
        }

        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .limit(nationalRegistriesConfig.getQueryLimit());

        if (!CollectionUtils.isEmpty(lastEvaluatedKey)) {
            builder.exclusiveStartKey(lastEvaluatedKey);
        }

        return Mono.from(table.index(NationalRegistriesRequest.BATCH_ID_INDEX).query(builder.build()));
    }

    /**
     * Recupera, in modo paginato, le richieste associate a uno specifico registro,
     * ordinate per data di ricezione, disponibili per la raccolta in batch
     * (stato = NOT_WORKED).
     */
    @Override
    public Mono<Page<NationalRegistriesRequest>> getRequestsByRegistryStatus(GatewayDownstreamService registry, Map<String, AttributeValue> lastEvaluatedKey, int limit) {

        String registryStatus = String.join("#", registry.name(), NOT_WORKED.name());

        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(registryStatus).build()))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastEvaluatedKey)) {
            builder.exclusiveStartKey(lastEvaluatedKey);
        }

        return Mono.from(table.index(NationalRegistriesRequest.REGISTRY_STATUS_CREATED_AT_INDEX).query(builder.build()));
    }

    /**
     * Aggiorna lo stato della richiesta a WORKING.
     * La transizione è consentita solo da NOT_WORKED.
     * Il batchId è obbligatorio.
     * Vengono aggiornati atomicamente:
     * - status
     * - registry
     * - registryStatus
     * - batchId
     * - ttl
     * - updatedAt
     */
    @Override
    public Mono<NationalRegistriesRequest> updateStatusToWorking(String pk, String sk, String batchId, GatewayDownstreamService registry) {

        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        if (!StringUtils.hasText(batchId)) {
            log.error("BatchId is required to set status to WORKING, but it is null or empty");
            throw new PnInternalException("BatchId is required to set status to WORKING", 400, ERROR_CODE_NATIONAL_REGISTRIES_REQUIRED_BATCHID);
        }

        names.put("#batchId", NationalRegistriesRequest.COL_BATCH_ID);
        values.put(":batchId", AttributeValue.builder().s(batchId).build());
        updates.add("#batchId = :batchId");

        addTtl(names, values, updates);

        return updateStatus(pk, sk, WORKING, registry, names, values, updates);
    }

    /**
     * Aggiorna lo stato della richiesta a WORKED
     * La transizione è consentita solo da WORKING.
     * Il resultPayload è obbligatorio.
     * WORKED è uno stato finale, quindi viene aggiornato anche il TTL.
     */
    @Override
    public Mono<NationalRegistriesRequest> updateStatusToWorked(String pk, String sk, String resultPayload, GatewayDownstreamService registry) {

        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        if (!StringUtils.hasText(resultPayload)) {
            log.error("ResultPayload is required to set status to WORKED, but it is null or empty");
            throw new PnInternalException("ResultPayload is required to set status to WORKED",400, ERROR_CODE_NATIONAL_REGISTRIES_REQUIRED_RESULTPAYLOAD);
        }

        names.put("#resultPayload", NationalRegistriesRequest.COL_RESULT_PAYLOAD);
        values.put(":resultPayload", AttributeValue.builder().s(resultPayload).build());
        updates.add("#resultPayload = :resultPayload");

        addTtl(names, values, updates);

        return updateStatus(pk, sk, WORKED, registry, names, values, updates);
    }

    /**
     * Aggiorna lo stato della richiesta a ERROR.
     * La transizione è consentita solo da WORKING.
     * ERROR è uno stato finale, quindi viene aggiornato anche il TTL.
     */
    @Override
    public Mono<NationalRegistriesRequest> updateStatusToError(String pk, String sk, GatewayDownstreamService registry) {

        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        addTtl(names, values, updates);

        return updateStatus(pk, sk, ERROR, registry, names, values, updates);
    }

    /**
     * Reimposta lo stato della richiesta a NOT_WORKED
     * per consentire un nuovo tentativo di elaborazione.
     * La transizione consentita è:
     * WORKING -> NOT_WORKED
     * Contestualmente aggiorna il contatore dei retry.
     */
    @Override
    public Mono<NationalRegistriesRequest> resetStatusForRetry(String pk, String sk, Integer retry, GatewayDownstreamService registry) {

        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();
        List<String> updates = new ArrayList<>();

        names.put("#retryCount", NationalRegistriesRequest.COL_RETRY_COUNT);
        values.put(":retryCount", AttributeValue.builder().n(String.valueOf(retry)).build());
        updates.add("#retryCount = :retryCount");

        return updateStatus(pk, sk, NOT_WORKED, registry, names, values, updates);
    }

    /**
     * Esegue l'aggiornamento condizionale dello stato della richiesta.
     * Ogni modifica dello status aggiorna contestualmente anche registryStatus
     * secondo la forma:
     * registry#status
     * Esempio:
     * ANPR#NOT_WORKED
     * ANPR#WORKING
     * ANPR#WORKED
     * ANPR#ERROR
     * Se viene fornito un registry, viene aggiornato atomicamente insieme
     * allo status e al registryStatus.
     */
    public Mono<NationalRegistriesRequest> updateStatus(String pk, String sk, RequestStatusEnum newStatus, GatewayDownstreamService registry, Map<String, String> names, Map<String, AttributeValue> values, List<String> updates) {

        RequestStatusEnum expectedStatus = retrieveExpectedStatus(newStatus);
        Instant now = Instant.now();
        names.put("#status", NationalRegistriesRequest.COL_STATUS);
        values.put(":expectedStatus", AttributeValue.builder().s(expectedStatus.name()).build());
        values.put(":newStatus", AttributeValue.builder().s(newStatus.name()).build());
        updates.add("#status = :newStatus");

        if (Objects.nonNull(registry)) {
            names.put("#registry", NationalRegistriesRequest.COL_REGISTRY);
            values.put(":registry", AttributeValue.builder().s(registry.name()).build());
            updates.add("#registry = :registry");

            String registryStatus = String.join("#", registry.name(), newStatus.name());

            names.put("#registryStatus", NationalRegistriesRequest.COL_REGISTRY_STATUS);
            values.put(":registryStatus", AttributeValue.builder().s(registryStatus).build());
            updates.add("#registryStatus = :registryStatus");
        }

        names.put("#updatedAt", NationalRegistriesRequest.COL_UPDATED_AT);
        values.put(":updatedAt", AttributeValue.builder().s(now.toString()).build());
        updates.add("#updatedAt = :updatedAt");

        UpdateItemRequest request = UpdateItemRequest.builder().tableName(nationalRegistriesConfig.getDao().getRequestsTableName())
                .key(Map.of(NationalRegistriesRequest.COL_CORRELATION_ID, AttributeValue.builder().s(pk).build(),
                                NationalRegistriesRequest.COL_CREATED_AT, AttributeValue.builder().s(sk).build()))
                .updateExpression("SET " + String.join(", ", updates))
                .conditionExpression("#status = :expectedStatus")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        try {
            return Mono.just(dynamoDbClient.updateItem(request))
                    .map(updateItemResponse -> NationalRegistriesRequest.attributeValueMapToNationalRegistriesRequest(updateItemResponse.attributes()));
        } catch (ConditionalCheckFailedException e) {
            log.error("Unable to transition to {}: the current status is not {}", newStatus, expectedStatus);
            throw new PnInternalException(String.format("Unable to transition to %s: the current status is not %s", newStatus, expectedStatus), 400, ERROR_CODE_NATIONAL_REGISTRIES_CONDITIONALCHECKFAILED);
        }
    }

    /**
     * Determina lo stato atteso prima della transizione.
     * Transizioni consentite:
     * NOT_WORKED -> WORKING
     * WORKING    -> WORKED
     * WORKING    -> ERROR
     * WORKING    -> NOT_WORKED
     */
    private RequestStatusEnum retrieveExpectedStatus(RequestStatusEnum newStatus) {
        return switch (newStatus) {
            case WORKING -> NOT_WORKED;
            case WORKED, ERROR, NOT_WORKED -> WORKING;
        };
    }

    private void addTtl(Map<String, String> names, Map<String, AttributeValue> values, List<String> updates) {
        Instant now = Instant.now();
        long ttl = now.plusSeconds(nationalRegistriesConfig.getTtl()).getEpochSecond();
        names.put("#ttl", NationalRegistriesRequest.COL_TTL);
        values.put(":ttl", AttributeValue.builder().n(Long.toString(ttl)).build());
        updates.add("#ttl = :ttl");
    }
}
