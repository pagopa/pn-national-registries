package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.logging.LogLevel;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalWebClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;

@Component
@Slf4j
public class AgenziaEntrateWebClientSOAP extends CommonWebClient {

    private final String basePath;
    private final AdeLegalWebClientConfig webClientConfig;

    public AgenziaEntrateWebClientSOAP(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                                       @Value("${pn.national.registries.ade-legal.base-path}") String basePath,
                                       AdeLegalWebClientConfig webClientConfig) {
        super(sslCertVer);
        this.basePath = basePath;
        this.webClientConfig = webClientConfig;
    }

    protected WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(webClientConfig.getTcpMaxPoolSize())
                .pendingAcquireMaxCount(webClientConfig.getTcpMaxQueuedConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.getTcpPendingAcquiredTimeout()))
                .maxIdleTime(Duration.ofMillis(webClientConfig.getTcpPoolIdleTimeout()))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

        return super.initWebClient(httpClient, basePath);
    }
}
