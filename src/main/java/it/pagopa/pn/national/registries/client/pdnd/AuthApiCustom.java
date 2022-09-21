package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.api.AuthApi;
import org.springframework.stereotype.Component;

@Component
public class AuthApiCustom extends AuthApi {

    public AuthApiCustom(PdndApiClient apiClient) {
        this.setApiClient(apiClient);
    }
}
