package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenCacheEntryTest {

    @Test
    void setClientCredentials() {
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        TokenType tokenType = TokenType.BEARER;
        ClientCredentialsResponse client = new ClientCredentialsResponse();
        client.setAccessToken("test");
        client.setTokenType(tokenType);
        accessTokenCacheEntry.setClientCredentials(client);
        Assertions.assertEquals("test",accessTokenCacheEntry.getTokenValue());
        Assertions.assertEquals("purposeId",accessTokenCacheEntry.getTokenKey());
        Assertions.assertEquals("Bearer",accessTokenCacheEntry.getTokenType().getValue());
    }
}
