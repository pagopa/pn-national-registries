package it.pagopa.pn.national.registries.client.pdnd;

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
public class PdndWebClient extends CommonWebClient {

    private final Integer tcpMaxPoolSize;
    private final Integer tcpMaxQueuedConnections;
    private final Integer tcpPendingAcquireTimeout;
    private final Integer tcpPoolIdleTimeout;
    private final String basePath;

    public PdndWebClient(@Value("${webclient.pdnd.tcp-max-poolsize}") Integer tcpMaxPoolSize,
                         @Value("${webclient.pdnd.tcp-max-queued-connections}") Integer tcpMaxQueuedConnections,
                         @Value("${webclient.pdnd.tcp-pending-acquired-timeout}") Integer tcpPendingAcquireTimeout,
                         @Value("${webclient.pdnd.tcp-pool-idle-timeout}") Integer tcpPoolIdleTimeout,
                         @Value("${pdnd.base-path}") String basePath) {
        this.tcpMaxPoolSize = tcpMaxPoolSize;
        this.tcpPendingAcquireTimeout = tcpPendingAcquireTimeout;
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
        this.basePath = basePath;
    }

    protected final WebClient initWebClient() {

        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(connectionProvider);

        return super.initWebClient(httpClient,basePath);
    }
}
