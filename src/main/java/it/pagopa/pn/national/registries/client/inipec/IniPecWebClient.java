package it.pagopa.pn.national.registries.client.inipec;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.net.ssl.SSLException;
import java.io.*;
import java.time.Duration;
import java.util.*;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_ADDRESS_ANPR;

@Component
@Slf4j
public class IniPecWebClient extends CommonWebClient {

    private final ObjectMapper objectMapper;
    private final SecretManagerService secretManagerService;

    private final String basePath;
    private final String purposeId;
    private Integer tcpMaxQueuedConnections;
    private Integer tcpPendingAcquiredTimeout;
    private Integer tcpPoolIdleTimeout;
    private Integer tcpMaxPoolSize;
    private String secretName;

    public IniPecWebClient(@Value("pn.national.registries.pdnd.inipec.base-path}") String basePath,
                         @Value("${pn.national.registries.pdnd.inipec.purpose-id}") String purposeId,
                         @Value("${pn.national.registries.webclient.inipec.tcp-max-poolsize}") Integer tcpMaxPoolSize,
                         @Value("${pn.national.registries.webclient.inipec.tcp-max-queued-connections}") Integer tcpMaxQueuedConnections,
                         @Value("${pn.national.registries.webclient.inipec.tcp-pending-acquired-timeout}") Integer tcpPendingAcquiredTimeout,
                         @Value("${pn.national.registries.webclient.inipec.tcp-pool-idle-timeout}") Integer tcpPoolIdleTimeout,
                         SecretManagerService secretsManagerService) {
        this.basePath = basePath;
        this.purposeId = purposeId;
        this.tcpMaxPoolSize = tcpMaxPoolSize;
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
        this.tcpPendingAcquiredTimeout = tcpPendingAcquiredTimeout;
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
        this.secretManagerService = secretsManagerService;
        this.objectMapper = new ObjectMapper();
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
