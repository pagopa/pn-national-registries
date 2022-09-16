package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.external.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.external.registries.generated.openapi.pdnd.client.v1.dto.TokenTypeDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@Slf4j
public class AccessTokenCacheEntry {
    private String accessToken;
    private TokenTypeDto tokenType;
    private final String purposeId;

    public AccessTokenCacheEntry(String purposeId) {
        this.purposeId = purposeId;
    }

    public void setClientCredentials(ClientCredentialsResponseDto clientCredential) {
        accessToken = clientCredential.getAccessToken();
        tokenType = clientCredential.getTokenType();
    }
}
