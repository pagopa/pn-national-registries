package it.pagopa.pn.national.registries.entity;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@DynamoDbBean
public class NationalRegistriesBatch {

    public static final String SK = "-";

    public static final String COL_BATCHID = "batchId";
    public static final String COL_SK = "sk";
    public static final String COL_CREATEDAT = "createdAt";
    public static final String COL_REGISTRY = "registry";
    public static final String COL_STATUS= "status";
    public static final String COL_POLLINGID = "pollingId";
    public static final String COL_PROVIDERLOCATION = "providerLocation";
    public static final String COL_PRACTICALREFERENCE = "practicalReference";
    public static final String COL_BATCHSIZE = "batchSize";
    public static final String COL_ATTEMPTAFTER = "attemptAfter";
    public static final String COL_ERRORRETRYCOUNT = "errorRetryCount";
    public static final String COL_INPROGRESSRETRYCOUNT = "inProgressRetryCount";
    public static final String COL_UPDATEDAT = "updatedAt";
    public static final String COL_TTL = "ttl";

    public static final String STATUS_ATTEMPTAFTER_INDEX = "status-attemptAfter-Index";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_BATCHID)}))
    private String batchId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))
    private String sk = "-";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATEDAT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTRY)}))
    private String registry;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS), @DynamoDbSecondaryPartitionKey(indexNames = STATUS_ATTEMPTAFTER_INDEX)}))
    private BatchesStatusEnum status;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_POLLINGID)}))
    private String pollingId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PROVIDERLOCATION)}))
    private String providerLocation;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRACTICALREFERENCE)}))
    private String practicalReferenceValue;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BATCHSIZE)}))
    private Integer batchSize;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ATTEMPTAFTER), @DynamoDbSecondarySortKey(indexNames = STATUS_ATTEMPTAFTER_INDEX)}))
    private Instant attemptAfter;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ERRORRETRYCOUNT)}))
    private Integer errorRetryCount;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_INPROGRESSRETRYCOUNT)}))
    private Integer inProgressRetryCount;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UPDATEDAT)}))
    private Instant updatedAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

    // Costruito UNA volta sola
    private static final TableSchema<NationalRegistriesBatch> SCHEMA =
            TableSchema.fromBean(NationalRegistriesBatch.class);

    /**
     * Converte l'entity NationalRegistriesBatch in una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB.
     */
    public static Map<String, AttributeValue> nationalRegistriesRequestToAttributeValueMap(NationalRegistriesBatch p) {
        return SCHEMA.itemToMap(p, true);
    }

    /**
     * Converte una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB in una entity NationalRegistriesRequest.
     */
    public static NationalRegistriesBatch attributeValueMapToNationalRegistriesRequest(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) return null;
        return SCHEMA.mapToItem(item);
    }
}
