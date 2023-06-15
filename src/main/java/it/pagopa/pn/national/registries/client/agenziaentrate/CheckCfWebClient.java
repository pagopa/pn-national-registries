package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class CheckCfWebClient extends CommonWebClient {

    private final String basePath;
    private final CheckCfWebClientConfig webClientConfig;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final String authChannelData;
    private final CheckCfSecretConfig checkCfSecretConfig;

    public CheckCfWebClient(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                            @Value("${pn.national.registries.ade-check-cf.base-path}") String basePath,
                            @Value("${pn.national.registries.ssm.ade-check-cf.auth-channel}") String authChannelData,
                            CheckCfWebClientConfig webClientConfig,
                            SsmParameterConsumerActivation ssmParameterConsumerActivation,
                            CheckCfSecretConfig checkCfSecretConfig) {
        super(sslCertVer);
        this.basePath = basePath;
        this.webClientConfig = webClientConfig;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.authChannelData = authChannelData;
        this.checkCfSecretConfig = checkCfSecretConfig;
    }

    protected WebClient init() {
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
            Optional<SSLData> optSslData = ssmParameterConsumerActivation.getParameterValue(authChannelData, SSLData.class);
            if(optSslData.isEmpty()) {
                throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF);
            }
            byte[] certificateBytes = Base64.getDecoder().decode(optSslData.get().getCert());
            InputStream inputStream = new ByteArrayInputStream(certificateBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate)cf.generateCertificate(inputStream);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType(), cf.getProvider());
            ks.load(null);
            ks.setCertificateEntry("caCert", caCert);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(ks, null);

            return getSslContext(SslContextBuilder.forClient()
                    .keyManager(keyManagerFactory), null);

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException |
                 UnrecoverableKeyException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}
