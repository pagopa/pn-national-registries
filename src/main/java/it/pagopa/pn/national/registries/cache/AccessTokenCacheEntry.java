package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AccessTokenCacheEntry {

    private String tokenValue;
    private TokenType tokenType;
    private String tokenKey;

    public AccessTokenCacheEntry(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public void setClientCredentials(ClientCredentialsResponse clientCredential) {
        tokenValue = clientCredential.getAccessToken();
        tokenType = clientCredential.getTokenType();
    }

    public void setClientCredentials(String tokenValue) {
        this.tokenValue = tokenValue;
        this.tokenType = TokenType.BEARER;
    }
}
