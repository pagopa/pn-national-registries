package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisConnectionWarmupTest {

    @Mock
    private ReactiveRedisConnectionFactory connectionFactory;
    @Mock
    private ReactiveRedisConnection reactiveRedisConnection;

    @Test
    void warmUpShouldPingRedisWhenConnectionIsAvailable() {
        when(connectionFactory.getReactiveConnection()).thenReturn(reactiveRedisConnection);
        when(reactiveRedisConnection.ping()).thenReturn(Mono.just("PONG"));

        RedisConnectionWarmup warmup = new RedisConnectionWarmup(connectionFactory);

        assertDoesNotThrow(warmup::warmUp);
        verify(reactiveRedisConnection).ping();
    }

    @Test
    void warmUpShouldIgnoreReactiveErrors() {
        when(connectionFactory.getReactiveConnection()).thenReturn(reactiveRedisConnection);
        when(reactiveRedisConnection.ping()).thenReturn(Mono.error(new RuntimeException("redis down")));

        RedisConnectionWarmup warmup = new RedisConnectionWarmup(connectionFactory);

        assertDoesNotThrow(warmup::warmUp);
        verify(reactiveRedisConnection).ping();
    }

    @Test
    void warmUpShouldIgnoreSynchronousExceptions() {
        when(connectionFactory.getReactiveConnection()).thenThrow(new RuntimeException("connection factory error"));

        RedisConnectionWarmup warmup = new RedisConnectionWarmup(connectionFactory);

        assertDoesNotThrow(warmup::warmUp);
    }
}

