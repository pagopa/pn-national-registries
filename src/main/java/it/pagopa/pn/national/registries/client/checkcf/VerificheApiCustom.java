package it.pagopa.pn.national.registries.client.checkcf;

import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.api.VerificheApi;
import org.springframework.stereotype.Component;

@Component
public class VerificheApiCustom extends VerificheApi {

    public VerificheApiCustom(CheckCfApiClient apiClient) {
        this.setApiClient(apiClient);
    }
}
