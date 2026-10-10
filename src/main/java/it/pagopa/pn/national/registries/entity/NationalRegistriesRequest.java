package it.pagopa.pn.national.registries.entity;

import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

@DynamoDbBean
@Data
public class NationalRegistriesRequest {

    public static final String COL_CORRELATION_ID = "correlationId";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_TAX_ID = "taxId";
    public static final String COL_RECIPIENT_TYPE = "recipientType";
    public static final String COL_DOMICILE_TYPE = "domicileType";
    public static final String COL_CLIENT_ID = "clientId";
    public static final String COL_REGISTRY = "registry";
    public static final String COL_STATUS = "status";
    public static final String COL_REGISTRY_STATUS = "registryStatus";
    public static final String COL_BATCH_ID = "batchId";
    public static final String COL_REFERENCE_REQUEST_DATE = "referenceRequestDate";
    public static final String COL_RESULT_PAYLOAD = "resultPayload";
    public static final String COL_UPDATED_AT = "updatedAt";
    public static final String COL_TTL = "ttl";

    public static final String BATCH_ID_INDEX = "batchIdIndex";
    public static final String REGISTRY_STATUS_CREATED_AT_INDEX = "registryStatusCreatedAtIndex";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_CORRELATION_ID)}))
    private String correlationId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_CREATED_AT), @DynamoDbSecondarySortKey(indexNames = REGISTRY_STATUS_CREATED_AT_INDEX)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TAX_ID)}))
    private String taxId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RECIPIENT_TYPE)}))
    private String recipientType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOMICILE_TYPE)}))
    private String domicileType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CLIENT_ID)}))
    private String clientId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTRY)}))
    private GatewayDownstreamService registry;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS)}))
    private RequestStatusEnum status;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTRY_STATUS), @DynamoDbSecondaryPartitionKey(indexNames = REGISTRY_STATUS_CREATED_AT_INDEX)}))
    private String registryStatus;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BATCH_ID), @DynamoDbSecondaryPartitionKey(indexNames = BATCH_ID_INDEX)}))
    private String batchId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REFERENCE_REQUEST_DATE)}))
    private String referenceRequestDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RESULT_PAYLOAD)}))
    private String resultPayload;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UPDATED_AT)}))
    private Instant updatedAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

    // Costruito UNA volta sola
    private static final TableSchema<NationalRegistriesRequest> SCHEMA =
            TableSchema.fromBean(NationalRegistriesRequest.class);

    /**
     * Converte l'entity NationalRegistriesRequest in una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB.
     */
    public static Map<String, AttributeValue> nationalRegistriesRequestToAttributeValueMap(NationalRegistriesRequest p) {
        return SCHEMA.itemToMap(p, true);
    }

    /**
     * Converte una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB in una entity NationalRegistriesRequest.
     */
    public static NationalRegistriesRequest attributeValueMapToNationalRegistriesRequest(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) return null;
        return SCHEMA.mapToItem(item);
    }
}