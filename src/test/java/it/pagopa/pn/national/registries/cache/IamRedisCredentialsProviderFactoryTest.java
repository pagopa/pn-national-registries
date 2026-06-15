package it.pagopa.pn.national.registries.cache;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class IamRedisCredentialsProviderFactoryTest {

    @Test
    void createCredentialsProviderShouldBuildServerlessIamProvider() {
        NationalRegistriesConfig.CacheConfigs cacheConfigs = buildCacheConfig(RedisMode.SERVERLESS);
        DefaultCredentialsProvider defaultCredentialsProvider = mock(DefaultCredentialsProvider.class);
        RedisCredentials expectedCredentials = RedisCredentials.just("redis-user", "signed-token");
        AtomicReference<Object> capturedUserId = new AtomicReference<>();
        AtomicReference<IAMAuthTokenRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<Object> capturedCredentialsProvider = new AtomicReference<>();

        try (MockedStatic<DefaultCredentialsProvider> mockedStatic = mockStatic(DefaultCredentialsProvider.class);
             MockedConstruction<RedisIAMAuthCredentialsProvider> mockedConstruction = mockConstruction(
                     RedisIAMAuthCredentialsProvider.class,
                     (mock, context) -> {
                         capturedUserId.set(context.arguments().get(0));
                         capturedRequest.set((IAMAuthTokenRequest) context.arguments().get(1));
                         capturedCredentialsProvider.set(context.arguments().get(2));
                         when(mock.getRedisCredentials()).thenReturn(expectedCredentials);
                     })) {

            mockedStatic.when(DefaultCredentialsProvider::create).thenReturn(defaultCredentialsProvider);

            IamRedisCredentialsProviderFactory factory = new IamRedisCredentialsProviderFactory(cacheConfigs);
            RedisCredentialsProvider provider = factory.createCredentialsProvider(new RedisStandaloneConfiguration());

            StepVerifier.create(provider.resolveCredentials())
                    .expectNext(expectedCredentials)
                    .verifyComplete();

            assertEquals(1, mockedConstruction.constructed().size());
            assertEquals("redis-user", capturedUserId.get());
            assertSame(defaultCredentialsProvider, capturedCredentialsProvider.get());
            assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.getField(capturedRequest.get(), "isServerless")));
            assertEquals("redis-user", ReflectionTestUtils.getField(capturedRequest.get(), "userId"));
            assertEquals("cache-name", ReflectionTestUtils.getField(capturedRequest.get(), "cacheName"));
            assertEquals("eu-south-1", ReflectionTestUtils.getField(capturedRequest.get(), "region"));
        }
    }

    @Test
    void createCredentialsProviderShouldBuildManagedIamProvider() {
        NationalRegistriesConfig.CacheConfigs cacheConfigs = buildCacheConfig(RedisMode.MANAGED);
        DefaultCredentialsProvider defaultCredentialsProvider = mock(DefaultCredentialsProvider.class);
        RedisCredentials expectedCredentials = RedisCredentials.just("redis-user", "signed-token");
        AtomicReference<IAMAuthTokenRequest> capturedRequest = new AtomicReference<>();

        try (MockedStatic<DefaultCredentialsProvider> mockedStatic = mockStatic(DefaultCredentialsProvider.class);
             MockedConstruction<RedisIAMAuthCredentialsProvider> mockedConstruction = mockConstruction(
                     RedisIAMAuthCredentialsProvider.class,
                     (mock, context) -> {
                         capturedRequest.set((IAMAuthTokenRequest) context.arguments().get(1));
                         when(mock.getRedisCredentials()).thenReturn(expectedCredentials);
                     })) {

            mockedStatic.when(DefaultCredentialsProvider::create).thenReturn(defaultCredentialsProvider);

            IamRedisCredentialsProviderFactory factory = new IamRedisCredentialsProviderFactory(cacheConfigs);
            RedisCredentialsProvider provider = factory.createCredentialsProvider(new RedisStandaloneConfiguration());

            StepVerifier.create(provider.resolveCredentials())
                    .expectNext(expectedCredentials)
                    .verifyComplete();

            assertEquals(1, mockedConstruction.constructed().size());
            assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.getField(capturedRequest.get(), "isServerless")));
        }
    }

    private NationalRegistriesConfig.CacheConfigs buildCacheConfig(RedisMode redisMode) {
        NationalRegistriesConfig.CacheConfigs cacheConfigs = new NationalRegistriesConfig.CacheConfigs();
        cacheConfigs.setUserId("redis-user");
        cacheConfigs.setCacheName("cache-name");
        cacheConfigs.setCacheRegion("eu-south-1");
        cacheConfigs.setMode(redisMode);
        return cacheConfigs;
    }
}
