package it.pagopa.pn.national.registries.entity;

import it.pagopa.pn.national.registries.constant.BatchRequestConstant;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.LocalDateTime;

@Data
@ToString
@DynamoDbBean
public class BatchPolling {

        @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("batchId")}))
        private String batchId;

        @Getter(onMethod=@__({@DynamoDbAttribute("pollingId")}))
        private String pollingId;

        @Getter(onMethod=@__({
                @DynamoDbAttribute("status"),
                @DynamoDbSecondaryPartitionKey(indexNames = BatchRequestConstant.GSI_S)
        }))
        private String status;

        @Getter(onMethod=@__({@DynamoDbAttribute("reservationId")}))
        private String reservationId;

        @Getter(onMethod=@__({@DynamoDbAttribute("timeStamp")}))
        private LocalDateTime timeStamp;

}
