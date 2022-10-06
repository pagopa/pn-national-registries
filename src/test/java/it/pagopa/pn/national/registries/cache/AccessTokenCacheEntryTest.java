package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.TokenTypeDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
        Assertions.assertEquals("test",accessTokenCacheEntry.getAccessToken());
        Assertions.assertEquals("purposeId",accessTokenCacheEntry.getPurposeId());
        Assertions.assertEquals("Bearer",accessTokenCacheEntry.getTokenType().getValue());
    }
}
