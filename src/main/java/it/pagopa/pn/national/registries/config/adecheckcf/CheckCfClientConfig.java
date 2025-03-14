package it.pagopa.pn.national.registries.config.adecheckcf;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CheckCfClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private final CheckCfSecretConfig checkCfSecretConfig;
    private final SecureWebClientUtils secureWebClientUtils;

    @Bean
    VerificheApi verificheApi(@Value("${pn.national.registries.ade-check-cf.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new VerificheApi(apiClient);
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient()
                .secure(t -> t.sslContext(buildSslContext()));
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(checkCfSecretConfig.getTrustData());
            return secureWebClientUtils.getSslContextForAde(SslContextBuilder.forClient(), trustData.getTrust());
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction();
    }
}
