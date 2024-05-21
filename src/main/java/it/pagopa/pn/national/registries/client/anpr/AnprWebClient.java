package it.pagopa.pn.national.registries.client.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.SecureWebClient;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Slf4j
@Component
public class AnprWebClient extends SecureWebClient {

    private final String basePath;
    private final AnprSecretConfig anprSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public AnprWebClient(@Value("${pn.national.registries.anpr.base-path}") String basePath,
                         AnprSecretConfig anprSecretConfig,
                         PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.basePath = basePath;
        this.anprSecretConfig = anprSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    protected WebClient init() {
        return super.initWebClient(basePath);
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(anprSecretConfig.getTrustSecret());
            return getSslContext(SslContextBuilder.forClient(), trustData.getTrust());
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }

}
