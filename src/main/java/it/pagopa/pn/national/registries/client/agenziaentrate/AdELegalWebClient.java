package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.CustomFormMessageWriter;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class AdELegalWebClient extends CommonBaseClient {

    private final String basePath;
    private final AdeLegalSecretConfig adeLegalSecretConfig;
    private final SecureWebClientUtils secureWebClientUtils;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public AdELegalWebClient(
            @Value("${pn.national.registries.ade-legal.base-path}") String basePath,
            AdeLegalSecretConfig adeLegalSecretConfig,
            SecureWebClientUtils secureWebClientUtils,
            PnNationalRegistriesSecretService pnNationalRegistriesSecretService
    ) {
        this.basePath = basePath;
        this.adeLegalSecretConfig = adeLegalSecretConfig;
        this.secureWebClientUtils = secureWebClientUtils;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public WebClient init() {
        return super.initWebClient(WebClient.builder().baseUrl(basePath)
                .codecs(c -> c.customCodecs().register(new CustomFormMessageWriter())));
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient().secure(t -> t.sslContext(buildSslContext()));
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(adeLegalSecretConfig.getTrustData());
            return secureWebClientUtils.getSslContext(SslContextBuilder.forClient(), trustData.getTrust());
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}
