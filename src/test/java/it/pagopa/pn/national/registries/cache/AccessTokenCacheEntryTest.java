package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenCacheEntryTest {

    @Test
    void setClientCredentials() {
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        TokenTypeDto tokenTypeDto = TokenTypeDto.BEARER;
        ClientCredentialsResponseDto client = new ClientCredentialsResponseDto();
        client.setAccessToken("test");
        client.setTokenType(tokenTypeDto);
        accessTokenCacheEntry.setClientCredentials(client);
        Assertions.assertEquals("test",accessTokenCacheEntry.getTokenValue());
        Assertions.assertEquals("purposeId",accessTokenCacheEntry.getTokenKey());
        Assertions.assertEquals("Bearer",accessTokenCacheEntry.getTokenType().getValue());
    }
}
