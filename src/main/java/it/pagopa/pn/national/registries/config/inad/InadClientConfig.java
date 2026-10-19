package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniMultipleApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InadClientConfig extends CommonBaseClient {
    private final WebClient.Builder builder;
    private final CustomRetryConfig customRetryConfig;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    @Bean
    ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi() {
        var apiClient = new ApiClient(initWebClient(this.builder, "INAD"));
        apiClient.setBasePath(nationalRegistriesConfig.getInadBaseURL());
        return new ApiEstrazioniPuntualiApi(apiClient);
    }

    @Bean
    ApiEstrazioniMultipleApi apiEstrazioniMultipleApi() {
        var apiClient = new ApiClient(initWebClient(this.builder, "INAD"));
        apiClient.setBasePath(nationalRegistriesConfig.getInadBaseURL());
        return new ApiEstrazioniMultipleApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction();
    }
}
