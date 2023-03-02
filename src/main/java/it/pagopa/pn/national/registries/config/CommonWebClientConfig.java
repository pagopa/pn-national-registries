package it.pagopa.pn.national.registries.config;

import lombok.Data;

@Data
public abstract class CommonWebClientConfig {
    private Integer tcpMaxPoolSize;
    private Integer tcpMaxQueuedConnections;
    private Integer tcpPendingAcquiredTimeout;
    private Integer tcpPoolIdleTimeout;
}
