package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.CustomFormMessageWriter;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class InadWebClient extends CommonBaseClient {

    private final String basePath;

    private final ResponseExchangeFilter responseExchangeFilter;

    public InadWebClient(@Value("${pn.national.registries.inad.base-path}") String basePath, ResponseExchangeFilter responseExchangeFilter) {
        this.basePath = basePath;
        this.responseExchangeFilter = responseExchangeFilter;
    }

    public WebClient init() {
        return super.enrichBuilder(WebClient.builder().baseUrl(basePath))
                .codecs(c -> c.customCodecs().register(new CustomFormMessageWriter()))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .build();
    }
}
