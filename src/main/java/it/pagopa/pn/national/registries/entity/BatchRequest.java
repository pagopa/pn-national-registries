package it.pagopa.pn.national.registries.entity;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

@Data
@ToString
@DynamoDbBean
public class BatchRequest {

        @Getter(onMethod=@__({
                @DynamoDbPartitionKey,
                @DynamoDbAttribute(BatchRequestConstant.PK)
        }))
        private String correlationId;

        @Getter(onMethod=@__({@DynamoDbAttribute("cf")}))
        private String cf;

        @Getter(onMethod = @__({
                @DynamoDbAttribute("batchId"),
                @DynamoDbSecondaryPartitionKey(indexNames = BatchRequestConstant.GSI_BL)
        }))
        private String batchId;

        @Getter(onMethod=@__({@DynamoDbAttribute("retry")}))
        private Integer retry;

        @Getter(onMethod=@__({@DynamoDbAttribute("ttl")}))
        private Long ttl;

        @Getter(onMethod=@__({
                @DynamoDbAttribute("status"),
                @DynamoDbSecondaryPartitionKey(indexNames = BatchRequestConstant.GSI_S)
        }))
        private String status;

        @Getter(onMethod=@__({
                @DynamoDbAttribute("lastReserved"),
                @DynamoDbSecondarySortKey(indexNames = BatchRequestConstant.GSI_BL)
        }))
        private LocalDateTime lastReserved;

        @Getter(onMethod=@__({
                @DynamoDbAttribute("timeStamp")
        }))
        private LocalDateTime timeStamp;
}
