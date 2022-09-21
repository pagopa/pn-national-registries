package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class InadApiClient extends ApiClient {
    public InadApiClient(InadWebClient webClient) {
        super(webClient.initWebClient());
    }
}
