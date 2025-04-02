package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@lombok.CustomLog
public class GatewayRequestTrackerRepositoryImpl implements GatewayRequestTrackerRepository {

    private final DynamoDbAsyncTable<GatewayRequestTrackerEntity> table;

    public GatewayRequestTrackerRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-gatewayRequestTracker", TableSchema.fromClass(GatewayRequestTrackerEntity.class));
    }

    @Override
    public Mono<GatewayRequestTrackerEntity> putIfAbsentOrRetrieve(String correlationId) {
        GatewayRequestTrackerEntity entity = buildEntity(correlationId);
        PutItemEnhancedRequest <GatewayRequestTrackerEntity> putItemEnhancedRequest = PutItemEnhancedRequest.builder(GatewayRequestTrackerEntity.class)
                .item(entity)
                .conditionExpression(Expression.builder().expression("attribute_not_exists(correlationId)").build())
                .build();

        return Mono.fromFuture(table.putItem(putItemEnhancedRequest))
                .thenReturn(entity)
                .onErrorResume(ConditionalCheckFailedException.class, t -> this.getItem(entity));
    }

    private GatewayRequestTrackerEntity buildEntity(String correlationId) {
        GatewayRequestTrackerEntity gatewayRequestTrackerEntity = new GatewayRequestTrackerEntity();
        gatewayRequestTrackerEntity.setCorrelationId(correlationId);
        gatewayRequestTrackerEntity.setRequestTimestamp(Instant.now());

        gatewayRequestTrackerEntity.setTtl(LocalDateTime.now().plusMonths(1).atZone(ZoneId.systemDefault()).toEpochSecond());
        return gatewayRequestTrackerEntity;
    }

    private Mono<GatewayRequestTrackerEntity> getItem(GatewayRequestTrackerEntity entity) {
        log.info("Item with correlationId {} already exists, trying to retrieve it", entity.getCorrelationId());
        Key key = Key.builder().partitionValue(entity.getCorrelationId()).build();
        return Mono.fromFuture(table.getItem(key));
    }

}
