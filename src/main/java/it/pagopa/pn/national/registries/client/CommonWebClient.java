package it.pagopa.pn.national.registries.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
public abstract class CommonWebClient extends CommonBaseClient {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    ResponseExchangeFilter responseExchangeFilter;

    protected CommonWebClient() {
    }

    protected final WebClient initWebClient(HttpClient httpClient,String baseUrl) {

        ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(configurer -> {
            configurer.registerDefaults(true);
            configurer.customCodecs().register(new CustomFormMessageWriter());
        }).build();

        return super.enrichBuilder(WebClient.builder()
                        .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .codecs(c ->
                        c.defaultCodecs().enableLoggingRequestDetails(true))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .clientConnector(new ReactorClientHttpConnector(httpClient))).build();
    }
}
