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
        String auditToken = "test auditToken";
        ClientCredentialsResponse client = new ClientCredentialsResponse();
        client.setAccessToken("test");
        accessTokenCacheEntry.setClientCredentials(client);
        accessTokenCacheEntry.setAuditToken(auditToken);
        Assertions.assertEquals("test",accessTokenCacheEntry.getBearerToken());
        Assertions.assertEquals("purposeId",accessTokenCacheEntry.getTokenKey());
        Assertions.assertEquals("test auditToken",accessTokenCacheEntry.getAuditToken());
    }
}
