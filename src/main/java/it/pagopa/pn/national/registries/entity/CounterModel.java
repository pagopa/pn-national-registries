package it.pagopa.pn.national.registries.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@ToString
@DynamoDbBean
public class CounterModel {

        @Setter
        @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("eservice")}))
        private String eservice;

        @Setter @Getter(onMethod=@__({@DynamoDbAtomicCounter(startValue = 1), @DynamoDbAttribute("counter")}))
        private Long counter;

}
