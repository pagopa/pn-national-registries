package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.AuthenticationApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class InfocamereAuthenticationClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;
    private final WebClient.Builder builder;

    public InfocamereAuthenticationClientConfig(CustomRetryConfig customRetryConfig, WebClient.Builder builder) {
        this.customRetryConfig = customRetryConfig;
        this.builder = builder;
    }

    @Bean
    AuthenticationApi authenticationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(builder, "INFOCAMERE-AUTH"));
        apiClient.setBasePath(basePath);
        return new AuthenticationApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction(CustomRetryConfig.infocamereRetryCondition);
    }
}
