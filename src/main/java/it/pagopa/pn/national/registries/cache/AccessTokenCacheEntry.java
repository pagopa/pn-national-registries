package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AccessTokenCacheEntry {

    private String accessToken;
    private TokenTypeDto tokenType;
    private String purposeId;

    public AccessTokenCacheEntry(String purposeId) {
        this.purposeId = purposeId;
    }

    public void setClientCredentials(ClientCredentialsResponseDto clientCredential) {
        accessToken = clientCredential.getAccessToken();
        tokenType = clientCredential.getTokenType();
    }
}
