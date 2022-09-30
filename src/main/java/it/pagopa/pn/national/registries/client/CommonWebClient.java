package it.pagopa.pn.national.registries.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Slf4j
public abstract class CommonWebClient extends CommonBaseClient {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    ResponseExchangeFilter responseExchangeFilter;

    protected CommonWebClient() {
    }

    protected final WebClient initWebClient(HttpClient httpClient) {

        ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(configurer -> {
            configurer.registerDefaults(true);
            configurer.customCodecs().register(new CustomFormMessageWriter());
        }).build();

        return super.enrichBuilder(WebClient.builder()
                .exchangeStrategies(strategies)
                .codecs(c ->
                        c.defaultCodecs().enableLoggingRequestDetails(true))
                .filters(exchangeFilterFunctions -> {
                    exchangeFilterFunctions.add(logRequest());
                    exchangeFilterFunctions.add(responseExchangeFilter);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))).build();
    }

    protected ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request HTTP {} to: {} - body: {}", clientRequest.method().name(), clientRequest.url(), clientRequest.body());
            return Mono.just(clientRequest);
        });
    }
}
