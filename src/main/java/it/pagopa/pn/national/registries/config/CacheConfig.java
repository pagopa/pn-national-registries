package it.pagopa.pn.national.registries.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import it.pagopa.pn.national.registries.cache.IamRedisCredentialsProviderFactory;
import it.pagopa.pn.national.registries.model.TokenPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CacheConfig {
    private final NationalRegistriesConfig pnNationalRegistriesConfig;

    @Bean
    @Primary
    @Profile("!local")
    public LettuceConnectionFactory elasticacheConnectionFactory() {
        NationalRegistriesConfig.CacheConfigs redisCache = pnNationalRegistriesConfig.getRedisCache();
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisCache.getHostName(), redisCache.getPort());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .redisCredentialsProviderFactory(new IamRedisCredentialsProviderFactory(redisCache))
                .useSsl()
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    @Profile("local")
    public LettuceConnectionFactory localConnectionFactory() {
        NationalRegistriesConfig.CacheConfigs redisCache = pnNationalRegistriesConfig.getRedisCache();
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisCache.getHostName(), redisCache.getPort());
        return new LettuceConnectionFactory(redisConfig, LettuceClientConfiguration.defaultConfiguration());
    }

    @Bean
    public ReactiveRedisTemplate<String, TokenPayload> reactiveRetrievalPayloadRedisTemplate(ReactiveRedisConnectionFactory elasticacheConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        Jackson2JsonRedisSerializer<TokenPayload> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, TokenPayload.class);
        RedisSerializationContext<String, TokenPayload> context =
                RedisSerializationContext.<String, TokenPayload>newSerializationContext(RedisSerializer.string())
                        .key(RedisSerializer.string())
                        .value(serializer)
                        .hashKey(RedisSerializer.string())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(elasticacheConnectionFactory, context);
    }

}

