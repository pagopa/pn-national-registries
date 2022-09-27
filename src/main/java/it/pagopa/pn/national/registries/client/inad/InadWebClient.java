package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "webclient.default")
@Slf4j
public class InadWebClient extends CommonBaseClient {

    private static final Integer TIMEOUT = 10000;

    @Value("${tcp-max-poolsize}")
    @NotNull
    @Min(value = 5)
    private String tcpMaxPoolSize;

    @Value("${tcp-max-queued-connections}")
    @NotNull
    @Min(value = 10)
    private String tcpMaxQueuedConnections;

    @Value("${tcp-pending-acquired-timeout}")
    @NotNull
    @Min(value = 45000)
    private String tcpPendingAcquiredTimeout;

    @Value("${tcp-pool-idle-timeout}")
    @NotNull
    @Min(value = 30000)
    private String tcpPoolIdleTimeout;

    public InadWebClient() {
        initWebClient();
    }

    protected final WebClient initWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(Integer.parseInt(tcpMaxPoolSize))
                .pendingAcquireMaxCount(Integer.parseInt(tcpMaxQueuedConnections))
                .pendingAcquireTimeout(Duration.ofMillis(Long.parseLong(tcpPendingAcquiredTimeout)))
                .maxIdleTime(Duration.ofMillis(Long.parseLong(tcpPoolIdleTimeout))).build();

        HttpClient httpClient = HttpClient.create(provider);

        return super.enrichBuilder(
                WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .filters(exchangeFilterFunctions -> {
                            exchangeFilterFunctions.add(logRequest());
                            exchangeFilterFunctions.add(logResponse());
                        })
        ).build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request HTTP {} {}", clientRequest.method().name(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response HTTP {} {}", clientResponse.statusCode().value(), clientResponse.statusCode().name());
            return Mono.just(clientResponse);
        });
    }

    public void setTcpMaxPoolSize(String tcpMaxPoolSize) {
        this.tcpMaxPoolSize = tcpMaxPoolSize;
    }

    public void setTcpMaxQueuedConnections(String tcpMaxQueuedConnections) {
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
    }

    public void setTcpPendingAcquiredTimeout(String tcpPendingAcquiredTimeout) {
        this.tcpPendingAcquiredTimeout = tcpPendingAcquiredTimeout;
    }

    public void setTcpPoolIdleTimeout(String tcpPoolIdleTimeout) {
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
    }
}
