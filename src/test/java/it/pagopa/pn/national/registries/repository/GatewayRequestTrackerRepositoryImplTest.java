package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GatewayRequestTrackerRepositoryImplTest {

    private GatewayRequestTrackerRepositoryImpl repository;
    private DynamoDbAsyncTable<GatewayRequestTrackerEntity> table;

    private static final String CORRELATION_ID = "correlationId";

    @BeforeEach
    void setUp() {
        DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedAsyncClient.class);
        table = Mockito.mock(DynamoDbAsyncTable.class);
        when(dynamoDbEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table);
        repository = new GatewayRequestTrackerRepositoryImpl(dynamoDbEnhancedClient);
    }

    @Test
    void putIfAbsentOrRetrieve_itemDoesNotExist_putsItem() {
        when(table.putItem(any(PutItemEnhancedRequest.class))).thenReturn(Mono.empty().toFuture());

        StepVerifier.create(repository.putIfAbsentOrRetrieve(CORRELATION_ID))
                .expectNextMatches(tracker -> {
                    Assertions.assertEquals(CORRELATION_ID, tracker.getCorrelationId());
                    Assertions.assertNotNull(tracker.getRequestTimestamp());
                    Assertions.assertNotNull(tracker.getTtl());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void putIfAbsentOrRetrieve_itemExists_retrievesExistingItem() {
        GatewayRequestTrackerEntity entity = new GatewayRequestTrackerEntity();

        when(table.putItem(any(PutItemEnhancedRequest.class))).thenReturn(
                Mono.error(ConditionalCheckFailedException
                    .builder()
                    .message("Fake exception")
                    .build()
                ).toFuture()
        );

        when(table.getItem(any(Key.class))).thenReturn(Mono.just(entity).toFuture());

        StepVerifier.create(repository.putIfAbsentOrRetrieve(CORRELATION_ID))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void putIfAbsentOrRetrieve_itemExists_retrieveFails() {
        when(table.putItem(any(PutItemEnhancedRequest.class))).thenReturn(Mono.error(new RuntimeException("Fake exception")).toFuture());

        StepVerifier.create(repository.putIfAbsentOrRetrieve(CORRELATION_ID))
                .expectError(RuntimeException.class)
                .verify();
    }
}