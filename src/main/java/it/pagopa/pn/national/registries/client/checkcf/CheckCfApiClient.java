package it.pagopa.pn.national.registries.client.checkcf;

import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class CheckCfApiClient extends ApiClient {

    public CheckCfApiClient(CheckCfWebClient webClient) {
        super(webClient.init());
    }
}
