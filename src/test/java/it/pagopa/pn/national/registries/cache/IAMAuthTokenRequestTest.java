package it.pagopa.pn.national.registries.cache;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IAMAuthTokenRequestTest {

    private static final AwsCredentials AWS_CREDENTIALS = AwsBasicCredentials.create("accessKey", "secretKey");

    @Test
    void toSignedRequestUriShouldIncludeServerlessResourceType() {
        IAMAuthTokenRequest request = new IAMAuthTokenRequest("redis-user", "cache-name", "eu-south-1", true);

        String signedUri = request.toSignedRequestUri(AWS_CREDENTIALS);

        assertFalse(signedUri.startsWith("http://"));
        assertTrue(signedUri.startsWith("cache-name/"));
        assertTrue(signedUri.contains("Action=connect"));
        assertTrue(signedUri.contains("User=redis-user"));
        assertTrue(signedUri.contains("ResourceType=ServerlessCache"));
        assertTrue(signedUri.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(signedUri.contains("X-Amz-Expires=900"));
        assertTrue(signedUri.contains("X-Amz-Signature="));
    }

    @Test
    void toSignedRequestUriShouldNotIncludeServerlessResourceTypeForManagedRedis() {
        IAMAuthTokenRequest request = new IAMAuthTokenRequest("redis-user", "cache-name", "eu-south-1", false);

        String signedUri = request.toSignedRequestUri(AWS_CREDENTIALS);

        assertFalse(signedUri.contains("ResourceType="));
        assertTrue(signedUri.contains("Action=connect"));
        assertTrue(signedUri.contains("User=redis-user"));
        assertTrue(signedUri.contains("X-Amz-Signature="));
    }
}

