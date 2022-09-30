package it.pagopa.pn.national.registries.client.inad;

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
public class InadWebClient extends CommonWebClient {

    @Value("${webclient.inad.tcp-max-poolsize}")
    Integer tcpMaxPoolSize;

    @Value("${webclient.inad.tcp-max-queued-connections}")
    Integer tcpMaxQueuedConnections;

    @Value("${webclient.inad.tcp-pending-acquired-timeout}")
    Integer tcpPendingAcquireTimeout;

    @Value("${webclient.inad.tcp-pool-idle-timeout}")
    Integer tcpPoolIdleTimeout;

    protected final WebClient initWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(provider);

        return super.initWebClient(httpClient);
    }
}
