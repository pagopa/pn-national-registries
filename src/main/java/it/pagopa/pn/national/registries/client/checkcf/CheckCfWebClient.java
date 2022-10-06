package it.pagopa.pn.national.registries.client.checkcf;

import io.netty.handler.logging.LogLevel;
import it.pagopa.pn.national.registries.client.CommonWebClient;
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
public class CheckCfWebClient extends CommonWebClient {

    @Value("${webclient.check-cf.tcp-max-poolsize}")
    Integer tcpMaxPoolSize;

    @Value("${webclient.check-cf.tcp-max-queued-connections}")
    Integer tcpMaxQueuedConnections;

    @Value("${webclient.check-cf.tcp-pending-acquired-timeout}")
    Integer tcpPendingAcquireTimeout;

    @Value("${webclient.check-cf.tcp-pool-idle-timeout}")
    Integer tcpPoolIdleTimeout;

    @Value("${pdnd.agenzia-entrate.base-path}")
    String basePath;

    protected WebClient init() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(provider).wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);;

        return super.initWebClient(httpClient,basePath);
    }
}
