package it.pagopa.pn.national.registries.config.pdnd;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PdndClientConfig extends CommonBaseClient {

    private final WebClient.Builder builder;

    @Bean
    AuthApi authApi(@Value("${pn.national.registries.pdnd.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(this.builder));
        apiClient.setBasePath(basePath);
        return new AuthApi(apiClient);
    }
}