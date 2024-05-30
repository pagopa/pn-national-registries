package it.pagopa.pn.national.registries.config.ipa;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.api.IpaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class IpaClientConfig extends CommonBaseClient {

    @Bean
    IpaApi ipaApi(@Value("${pn.national.registries.ipa.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new IpaApi(apiClient);
    }
}
