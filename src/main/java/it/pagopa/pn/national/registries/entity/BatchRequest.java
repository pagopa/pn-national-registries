package it.pagopa.pn.national.registries.entity;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

import static it.pagopa.pn.national.registries.constant.BatchRequestConstant.*;

@Data
@ToString
@DynamoDbBean
public class BatchRequest {

        @Getter(onMethod = @__({
                @DynamoDbPartitionKey,
                @DynamoDbAttribute(PK)
        }))
        private String correlationId;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_CF)
        }))
        private String cf;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_BATCH_ID),
                @DynamoDbSecondaryPartitionKey(indexNames = GSI_BL)
        }))
        private String batchId;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_RETRY)
        }))
        private Integer retry;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_TTL)
        }))
        private Long ttl;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_CLIENT_ID)
        }))
        private String clientId;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_STATUS),
                @DynamoDbSecondaryPartitionKey(indexNames = BatchRequestConstant.GSI_S)
        }))
        private String status;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_LAST_RESERVED),
                @DynamoDbSecondarySortKey(indexNames = GSI_BL)
        }))
        private LocalDateTime lastReserved;

        @Getter(onMethod = @__({
                @DynamoDbAttribute(COL_TIMESTAMP)
        }))
        private LocalDateTime timeStamp;
}
