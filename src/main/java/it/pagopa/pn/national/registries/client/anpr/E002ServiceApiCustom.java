package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.api.E002ServiceApi;
import org.springframework.stereotype.Component;

@Component
public class E002ServiceApiCustom extends E002ServiceApi {

    public E002ServiceApiCustom(AnprApiClient apiClient) {
        this.setApiClient(apiClient);
    }
}
