package it.pagopa.pn.national.registries.entity;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;

@Data
@ToString
@DynamoDbBean
public class BatchPolling {

        @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("batchId")}))
        private String batchId;

        @Getter(onMethod=@__({@DynamoDbAttribute("pollingId")}))
        private String pollingId;

        @Getter(onMethod=@__({@DynamoDbAttribute("status")}))
        private String status;

}
