package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InfocamereClientConfig extends CommonBaseClient {

    @Bean
    AuthenticationApi authenticationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new AuthenticationApi(apiClient);
    }

    @Bean
    ApiImpreseRappresentateElencoApi legalRepresentativeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiImpreseRappresentateElencoApi(apiClient);
    }

    @Bean
    ApiRecuperoElencoPecApi pecApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiRecuperoElencoPecApi(apiClient);
    }

    @Bean
    ApiRecuperoSedeApi sedeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiRecuperoSedeApi(apiClient);
    }

    @Bean
    ApiRichiestaElencoPecApi apiRichiestaElencoPecApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiRichiestaElencoPecApi(apiClient);
    }
}
