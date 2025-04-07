package it.pagopa.pn.national.registries.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Setter
@Data
@ToString
@DynamoDbBean
public class GatewayRequestTrackerEntity {
    private static final String COL_CORRELATION_ID = "correlationId";
    private static final String COL_REQUEST_TIMESTAMP = "requestTimestamp";
    private static final String COL_TTL = "ttl";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_CORRELATION_ID)}))
    private String correlationId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_REQUEST_TIMESTAMP)}))
    private Instant requestTimestamp;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

}
