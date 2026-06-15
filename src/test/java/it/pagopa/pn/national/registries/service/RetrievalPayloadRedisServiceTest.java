package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.model.TokenPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalPayloadRedisServiceTest {

    private static final String RETRIEVAL_ID = "retrieval-id";
    private static final String CACHE_KEY = "pn-emd::retrievalPayload::" + RETRIEVAL_ID;

    @Mock
    private ReactiveRedisTemplate<String, TokenPayload> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, TokenPayload> valueOperations;
    @Mock
    private TokenPayload payload;

    private RetrievalPayloadRedisService service;

    @BeforeEach
    void setUp() {
        service = new RetrievalPayloadRedisService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getShouldReturnPayloadUsingComposedKey() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(Mono.just(payload));

        StepVerifier.create(service.get(RETRIEVAL_ID))
                .expectNext(payload)
                .verifyComplete();

        verify(valueOperations).get(CACHE_KEY);
    }

    @Test
    void getShouldCompleteEmptyWhenValueIsMissing() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(Mono.empty());

        StepVerifier.create(service.get(RETRIEVAL_ID))
                .verifyComplete();

        verify(valueOperations).get(CACHE_KEY);
    }

    @Test
    void getShouldSwallowRedisErrors() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(Mono.error(new RuntimeException("redis down")));

        StepVerifier.create(service.get(RETRIEVAL_ID))
                .verifyComplete();

        verify(valueOperations).get(CACHE_KEY);
    }

    @Test
    void setShouldStorePayloadUsingComposedKey() {
        when(valueOperations.set(CACHE_KEY, payload)).thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(service.set(RETRIEVAL_ID, payload))
                .verifyComplete();

        verify(valueOperations).set(CACHE_KEY, payload);
    }

    @Test
    void setWithTtlShouldStorePayloadUsingProvidedDuration() {
        Duration ttl = Duration.ofMinutes(5);
        when(valueOperations.set(CACHE_KEY, payload, ttl)).thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(service.set(RETRIEVAL_ID, payload, ttl))
                .verifyComplete();

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq(CACHE_KEY), org.mockito.ArgumentMatchers.eq(payload), ttlCaptor.capture());
        assertEquals(ttl, ttlCaptor.getValue());
    }

    @Test
    void setShouldCompleteWhenRedisFails() {
        when(valueOperations.set(CACHE_KEY, payload)).thenReturn(Mono.error(new RuntimeException("redis down")));

        StepVerifier.create(service.set(RETRIEVAL_ID, payload))
                .verifyComplete();

        verify(valueOperations).set(CACHE_KEY, payload);
    }

    @Test
    void deleteShouldRemovePayloadUsingComposedKey() {
        when(valueOperations.getAndDelete(CACHE_KEY)).thenReturn(Mono.just(payload));

        StepVerifier.create(service.delete(RETRIEVAL_ID))
                .verifyComplete();

        verify(valueOperations).getAndDelete(CACHE_KEY);
    }

    @Test
    void deleteShouldCompleteWhenRedisFails() {
        when(valueOperations.getAndDelete(CACHE_KEY)).thenReturn(Mono.error(new RuntimeException("redis down")));

        StepVerifier.create(service.delete(RETRIEVAL_ID))
                .verifyComplete();

        verify(valueOperations).getAndDelete(CACHE_KEY);
    }
}

