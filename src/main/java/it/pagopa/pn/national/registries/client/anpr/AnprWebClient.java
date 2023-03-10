package it.pagopa.pn.national.registries.client.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.config.anpr.AnprWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.time.Duration;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ADDRESS_ANPR;

@Component
@Slf4j
public class AnprWebClient extends CommonWebClient {

    private final String basePath;
    private final AnprSecretConfig anprSecretConfig;
    private final AnprWebClientConfig webClientConfig;

    public AnprWebClient(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                         @Value("${pn.national.registries.anpr.base-path}") String basePath,
                         AnprSecretConfig anprSecretConfig,
                         AnprWebClientConfig webClientConfig) {
        super(sslCertVer);
        this.basePath = basePath;
        this.anprSecretConfig = anprSecretConfig;
        this.webClientConfig = webClientConfig;
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
            SSLData sslData = anprSecretConfig.getAnprAuthChannelSecret();
            SslContextBuilder sslContext = SslContextBuilder.forClient()
                    .keyManager(getCertInputStream(sslData.getCert()), getKeyInputStream(sslData.getKey()));
            return getSslContext(sslContext, sslData);
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR, e);
        }
    }
}
