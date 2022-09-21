package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class PdndApiClient extends ApiClient {

    public PdndApiClient(PdndWebClient webClient) {
        super(webClient.initWebClient());
    }

}
