package it.pagopa.pn.national.registries.cache;

import io.micrometer.common.lang.Nullable;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AccessTokenCacheEntry {

    @NotNull
    private String bearerToken;
    @Nullable
    private String auditToken;
    @NotNull
    private String tokenKey;

    public AccessTokenCacheEntry(@NotNull String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public void setClientCredentials(ClientCredentialsResponse clientCredential) {
        bearerToken = clientCredential.getAccessToken();
    }

    public void setClientCredentials(@NotNull String bearerToken) {
        this.bearerToken = bearerToken;
    }
}
