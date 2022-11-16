package it.pagopa.pn.national.registries.entity;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

@Data
@ToString
@DynamoDbBean
public class BatchRequest {

        @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("correlationId")}))
        private String correlationId;

        @Getter(onMethod=@__({@DynamoDbAttribute("cf")}))
        private String cf;

        @Getter(onMethod = @__({@DynamoDbAttribute("batchId")}))
        private String batchId;

        @Getter(onMethod=@__({@DynamoDbAttribute("retry")}))
        private String retry;

        @Getter(onMethod=@__({@DynamoDbAttribute("ttl")}))
        private LocalDateTime ttl;

        @Getter(onMethod=@__({@DynamoDbAttribute("status")}))
        private String status;

        @Getter(onMethod=@__({@DynamoDbAttribute("lastReserved")}))
        private LocalDateTime lastReserved;

        @Getter(onMethod=@__({@DynamoDbAttribute("timeStamp")}))
        private LocalDateTime timeStamp;
}
