package it.pagopa.pn.national.registries.cache;

import io.lettuce.core.RedisCredentialsProvider;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.lettuce.RedisCredentialsProviderFactory;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Factory that creates an IAM-authenticated {@link RedisCredentialsProvider} for Lettuce,
 * using configuration from {@link NationalRegistriesConfig.CacheConfigs}.
 */
public class IamRedisCredentialsProviderFactory implements RedisCredentialsProviderFactory {

    private final NationalRegistriesConfig.CacheConfigs cacheConfigs;

    public IamRedisCredentialsProviderFactory(NationalRegistriesConfig.CacheConfigs cacheConfigs) {
        this.cacheConfigs = cacheConfigs;
    }

    @Override
    public RedisCredentialsProvider createCredentialsProvider(RedisConfiguration redisConfiguration) {
        boolean isServerless = cacheConfigs.getMode() == RedisMode.SERVERLESS;
        IAMAuthTokenRequest iamRequest = new IAMAuthTokenRequest(
                cacheConfigs.getUserId(),
                cacheConfigs.getCacheName(),
                cacheConfigs.getCacheRegion(),
                isServerless
        );
        RedisIAMAuthCredentialsProvider provider = new RedisIAMAuthCredentialsProvider(
                cacheConfigs.getUserId(),
                iamRequest,
                DefaultCredentialsProvider.create()
        );
        return () -> Mono.fromSupplier(provider::getRedisCredentials);
    }
}

