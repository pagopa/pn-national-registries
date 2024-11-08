package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.ApiClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InadClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;

    @Bean
    ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi(@Value("${pn.national.registries.inad.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiEstrazioniPuntualiApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction();
    }
}
