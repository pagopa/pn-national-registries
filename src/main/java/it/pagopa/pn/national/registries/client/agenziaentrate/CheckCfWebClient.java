package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.amazonaws.util.StringUtils;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class CheckCfWebClient extends CommonWebClient {

    private final Integer tcpMaxPoolSize;
    private final Integer tcpMaxQueuedConnections;
    private final Integer tcpPendingAcquireTimeout;
    private final Integer tcpPoolIdleTimeout;
    private final String basePath;

    private final CheckCfSecretConfig checkCfSecretConfig;

    public CheckCfWebClient(@Value("${pn.national.registries.webclient.check-cf.tcp-max-poolsize}") Integer tcpMaxPoolSize,
                            @Value("${pn.national.registries.webclient.check-cf.tcp-max-queued-connections}") Integer tcpMaxQueuedConnections,
                            @Value("${pn.national.registries.webclient.check-cf.tcp-pending-acquired-timeout}") Integer tcpPendingAcquireTimeout,
                            @Value("${pn.national.registries.webclient.check-cf.tcp-pool-idle-timeout}") Integer tcpPoolIdleTimeout,
                            @Value("${pn.national.registries.pdnd.agenzia-entrate.base-path}") String basePath,
                            CheckCfSecretConfig checkCfSecretConfig) {
        this.tcpMaxPoolSize = tcpMaxPoolSize;
        this.tcpPendingAcquireTimeout = tcpPendingAcquireTimeout;
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
        this.basePath = basePath;
        this.checkCfSecretConfig = checkCfSecretConfig;
    }

    protected WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .secure(t -> t.sslContext(buildSSLHttpClient()))
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

        return super.initWebClient(httpClient, basePath);
    }

    public SslContext buildSSLHttpClient() {
        try {
            SSLData sslData = checkCfSecretConfig.getCheckCfAuthChannelSecret();
            SslContextBuilder sslContext = SslContextBuilder.forClient()
                    .keyManager(getCertInputStream(sslData.getCert()),getKeyInputStream(sslData.getKey()));

            return getSslContext(sslContext,sslData);

        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF,e);
        }
    }

    public SslContext getSslContext(SslContextBuilder sslContextBuilder, SSLData sslData) throws SSLException {
        if(StringUtils.isNullOrEmpty(sslData.getTrust())){
            return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
        return sslContextBuilder.trustManager(getTrustCertInputStream(sslData.getTrust())).build();
    }

    public InputStream getTrustCertInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }

    private InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }

    private InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(stringCert));
    }
}
