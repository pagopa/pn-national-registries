package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class AnprApiClient extends ApiClient {

    public AnprApiClient(AnprWebClient webClient) {
        super(webClient.initWebClient());
    }
}
