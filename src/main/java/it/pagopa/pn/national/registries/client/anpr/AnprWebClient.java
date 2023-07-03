package it.pagopa.pn.national.registries.client.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.config.anpr.AnprWebClientConfig;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.io.IOException;
import java.time.Duration;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Slf4j
@Component
public class AnprWebClient extends CommonWebClient {

    private final String basePath;
    private final AnprWebClientConfig webClientConfig;
    private final AnprSecretConfig anprSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public AnprWebClient(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                         @Value("${pn.national.registries.anpr.base-path}") String basePath,
                         AnprWebClientConfig webClientConfig,
                         AnprSecretConfig anprSecretConfig,
                         PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        super(sslCertVer);
        this.basePath = basePath;
        this.webClientConfig = webClientConfig;
        this.anprSecretConfig = anprSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(webClientConfig.getTcpMaxPoolSize())
                .pendingAcquireMaxCount(webClientConfig.getTcpMaxQueuedConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.getTcpPendingAcquiredTimeout()))
                .maxIdleTime(Duration.ofMillis(webClientConfig.getTcpPoolIdleTimeout()))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .secure(t -> t.sslContext(buildSslContext()));

        return super.initWebClient(httpClient, basePath);
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
