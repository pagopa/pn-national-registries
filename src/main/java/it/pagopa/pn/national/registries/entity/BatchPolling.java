package it.pagopa.pn.national.registries.entity;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

import static it.pagopa.pn.national.registries.constant.BatchPollingConstant.*;

@Data
@ToString
@DynamoDbBean
public class BatchPolling {

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(PK)
    }))
    private String batchId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_POLLING_ID)
    }))
    private String pollingId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RETRY)
    }))
    private Integer retry;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RETRY_IN_PROGRESS)
    }))
    private Integer inProgressRetry;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TTL)
    }))
    private Long ttl;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_S)
    }))
    private String status;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RESERVATION_ID)
    }))
    private String reservationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_LAST_RESERVED)
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TIMESTAMP)
    }))
    private LocalDateTime createdAt;
}
