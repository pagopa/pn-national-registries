package it.pagopa.pn.national.registries.cache;

import io.lettuce.core.RedisCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisIAMAuthCredentialsProviderTest {

    @Mock
    private IAMAuthTokenRequest iamAuthTokenRequest;
    @Mock
    private AwsCredentialsProvider awsCredentialsProvider;
    @Mock
    private AwsCredentials awsCredentials;

    @Test
    void constructorShouldPreGenerateTokenAndReuseCachedCredentials() {
        when(awsCredentialsProvider.resolveCredentials()).thenReturn(awsCredentials);
        when(iamAuthTokenRequest.toSignedRequestUri(awsCredentials)).thenReturn("token-1");

        RedisIAMAuthCredentialsProvider provider = new RedisIAMAuthCredentialsProvider(
                "redis-user",
                iamAuthTokenRequest,
                awsCredentialsProvider
        );

        RedisCredentials redisCredentials = provider.getRedisCredentials();

        assertEquals("redis-user", redisCredentials.getUsername());
        assertEquals("token-1", new String(redisCredentials.getPassword()));
        verify(awsCredentialsProvider, times(1)).resolveCredentials();
        verify(iamAuthTokenRequest, times(1)).toSignedRequestUri(awsCredentials);
    }

    @Test
    void getRedisCredentialsShouldRefreshTokenWhenExpired() {
        when(awsCredentialsProvider.resolveCredentials()).thenReturn(awsCredentials);
        when(iamAuthTokenRequest.toSignedRequestUri(awsCredentials)).thenReturn("token-1", "token-2");

        RedisIAMAuthCredentialsProvider provider = new RedisIAMAuthCredentialsProvider(
                "redis-user",
                iamAuthTokenRequest,
                awsCredentialsProvider
        );
        ReflectionTestUtils.setField(provider, "tokenExpiry", Instant.EPOCH);

        RedisCredentials redisCredentials = provider.getRedisCredentials();

        assertEquals("redis-user", redisCredentials.getUsername());
        assertEquals("token-2", new String(redisCredentials.getPassword()));
        verify(awsCredentialsProvider, times(2)).resolveCredentials();
        verify(iamAuthTokenRequest, times(2)).toSignedRequestUri(awsCredentials);
    }
}

