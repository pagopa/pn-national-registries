package it.pagopa.pn.national.registries.client;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.encoding.Jaxb2SoapDecoder;
import it.pagopa.pn.national.registries.client.agenziaentrate.encoding.Jaxb2SoapEncoder;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
public abstract class CommonWebClientSOAP extends CommonBaseClient {

    @Autowired
    ResponseExchangeFilter responseExchangeFilter;

    protected final WebClient initWebClient(HttpClient httpClient,String baseUrl) {

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder().codecs( clientCodecConfigurer -> {
            clientCodecConfigurer.customCodecs().register(new Jaxb2SoapEncoder());
            clientCodecConfigurer.customCodecs().register(new Jaxb2SoapDecoder());
        }).build();

        return super.enrichBuilder(WebClient.builder()
                        .baseUrl(baseUrl)
                .exchangeStrategies(exchangeStrategies)
                .codecs(c ->
                        c.defaultCodecs().enableLoggingRequestDetails(true))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .clientConnector(new ReactorClientHttpConnector(httpClient))).build();
    }
}
