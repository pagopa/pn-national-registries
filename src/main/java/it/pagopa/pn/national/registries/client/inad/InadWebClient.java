package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.inad.InadWebClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Slf4j
@Component
public class InadWebClient extends CommonWebClient {

    private final String basePath;
    private final InadWebClientConfig webClientConfig;

    public InadWebClient(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                         @Value("${pn.national.registries.inad.base-path}") String basePath,
                         InadWebClientConfig webClientConfig) {
        super(sslCertVer);
        this.basePath = basePath;
        this.webClientConfig = webClientConfig;
    }

    public WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(webClientConfig.getTcpMaxPoolSize())
                .pendingAcquireMaxCount(webClientConfig.getTcpMaxQueuedConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.getTcpPendingAcquiredTimeout()))
                .maxIdleTime(Duration.ofMillis(webClientConfig.getTcpPoolIdleTimeout()))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider);

        return super.initWebClient(httpClient, basePath);
    }
}
