package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.api.ApiEstrazioniPuntualiApi;
import org.springframework.stereotype.Component;

@Component
public class EstrazioniPuntualiApiCustom extends ApiEstrazioniPuntualiApi {

    public EstrazioniPuntualiApiCustom(InadApiClient apiClient) {
        this.setApiClient(apiClient);
    }
}
