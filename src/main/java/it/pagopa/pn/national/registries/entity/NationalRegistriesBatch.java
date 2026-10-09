package it.pagopa.pn.national.registries.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@NoArgsConstructor
@DynamoDbBean
public class NationalRegistriesBatch {

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute("batchId")}))
    private String batchId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute("sk")}))
    private String sk = "-";

    @Getter(onMethod = @__({@DynamoDbAttribute("createdAt")}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute("registry")}))
    private String registry;

    @Getter(onMethod = @__({@DynamoDbAttribute("status"), @DynamoDbSecondaryPartitionKey(indexNames = "StatusAttemptAfterIndex")}))
    private String status;

    @Getter(onMethod = @__({@DynamoDbAttribute("pollingId")}))
    private String pollingId;

    @Getter(onMethod = @__({@DynamoDbAttribute("providerLocation")}))
    private String providerLocation;

    @Getter(onMethod = @__({@DynamoDbAttribute("practicalReferenceValue")}))
    private String practicalReferenceValue;

    @Getter(onMethod = @__({@DynamoDbAttribute("batchSize")}))
    private Integer batchSize;

    @Getter(onMethod = @__({@DynamoDbAttribute("attemptAfter"), @DynamoDbSecondarySortKey(indexNames = "StatusAttemptAfterIndex")}))
    private Instant attemptAfter;

    @Getter(onMethod = @__({@DynamoDbAttribute("retryCount")}))
    private Integer retryCount;

    @Getter(onMethod = @__({@DynamoDbAttribute("inProgressRetryCount")}))
    private Integer inProgressRetryCount;

    @Getter(onMethod = @__({@DynamoDbAttribute("updatedAt")}))
    private Instant updatedAt;

    @Getter(onMethod = @__({@DynamoDbAttribute("ttl")}))
    private Long ttl;
}
