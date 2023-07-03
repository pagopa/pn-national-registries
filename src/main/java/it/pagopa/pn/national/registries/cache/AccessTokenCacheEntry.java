package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AccessTokenCacheEntry {

    private String tokenValue;
    private TokenTypeDto tokenType;
    private String tokenKey;

    public AccessTokenCacheEntry(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public void setClientCredentials(ClientCredentialsResponseDto clientCredential) {
        tokenValue = clientCredential.getAccessToken();
        tokenType = clientCredential.getTokenType();
    }

    public void setClientCredentials(String tokenValue) {
        this.tokenValue = tokenValue;
        this.tokenType = TokenTypeDto.BEARER;
    }
}
