package it.pagopa.pn.national.registries.config.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AnprClientConfig extends CommonBaseClient {

    private final SecureWebClientUtils secureWebClientUtils;
    private final AnprSecretConfig anprSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private final WebClient.Builder builder;

    @Bean
    E002ServiceApi e002ServiceApi(@Value("${pn.national.registries.anpr.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(this.builder));
        apiClient.setBasePath(basePath);
        return new E002ServiceApi(apiClient);
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient()
                .secure(t -> t.sslContext(buildSslContext()));
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(anprSecretConfig.getTrustSecret());
            return secureWebClientUtils.getSslContext(SslContextBuilder.forClient(), trustData.getTrust());
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}
