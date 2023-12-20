package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class PdndWebClient extends CommonBaseClient {
    private final String basePath;
    private final ResponseExchangeFilter responseExchangeFilter;

    public PdndWebClient(
            @Value("${pn.national.registries.pdnd.base-path}") String basePath, ResponseExchangeFilter responseExchangeFilter) {
        this.basePath = basePath;
        this.responseExchangeFilter = responseExchangeFilter;
    }

    public WebClient init() {
        return super.enrichBuilder(WebClient.builder().baseUrl(basePath))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .build();
    }
}
