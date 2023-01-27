package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Component
@Slf4j
public class InfoCamereWebClient extends CommonWebClient {

    private final String basePath;

    private final Integer tcpMaxQueuedConnections;
    private final Integer tcpPendingAcquiredTimeout;
    private final Integer tcpPoolIdleTimeout;
    private final Integer tcpMaxPoolSize;

    public InfoCamereWebClient(@Value("${pn.national.registries.infocamere.base-path}") String basePath,
                               @Value("${pn.national.registries.webclient.infocamere.tcp-max-poolsize}") Integer tcpMaxPoolSize,
                               @Value("${pn.national.registries.webclient.infocamere.tcp-max-queued-connections}") Integer tcpMaxQueuedConnections,
                               @Value("${pn.national.registries.webclient.infocamere.tcp-pending-acquired-timeout}") Integer tcpPendingAcquiredTimeout,
                               @Value("${pn.national.registries.webclient.infocamere.tcp-pool-idle-timeout}") Integer tcpPoolIdleTimeout) {
        this.basePath = basePath;
        this.tcpMaxPoolSize = tcpMaxPoolSize;
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
        this.tcpPendingAcquiredTimeout = tcpPendingAcquiredTimeout;
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
    }

    public WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquiredTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(connectionProvider);

        return super.initWebClient(httpClient,basePath);
    }

}
