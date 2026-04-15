package it.pagopa.pn.national.registries.config.infocamere;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.PecApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Configuration
@Slf4j
public class PecClientConfig extends CommonBaseClient {
    private final CustomRetryConfig customRetryConfig;
    private final WebClient pecWebClient;
    private final int maxRetryAttempts;
    private final boolean shouldRetryOnTimeout;

    public PecClientConfig(
            CustomRetryConfig customRetryConfig,
            WebClient.Builder builder,
            @Value("${pn.national.registries.infocamere.pec.retry.max-attempts}") int maxRetryAttempts,
            @Value("${pn.national.registries.infocamere.pec.retry.on-timeout}") boolean shouldRetryOnTimeout

    ) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.shouldRetryOnTimeout = shouldRetryOnTimeout;
        this.customRetryConfig = customRetryConfig;
        this.pecWebClient = initWebClient(builder);
    }

    @Bean
    PecApi pecApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(pecWebClient);
        apiClient.setBasePath(basePath);
        return new PecApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction(this::retryCondition, this.maxRetryAttempts);
    }

    public boolean retryCondition(Throwable throwable) {
        // No retry per timeout
        return throwable instanceof TimeoutException ||
                throwable instanceof SocketException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof SSLHandshakeException ||
                throwable instanceof UnknownHostException ||
                handleWebClientRequestException(throwable) ||
                throwable instanceof WebClientResponseException.TooManyRequests ||
                throwable instanceof WebClientResponseException.GatewayTimeout ||
                throwable instanceof WebClientResponseException.BadGateway ||
                throwable instanceof WebClientResponseException.InternalServerError ||
                throwable instanceof WebClientResponseException.ServiceUnavailable
                ;
    }

    private boolean handleWebClientRequestException(Throwable throwable) {
        if(this.shouldRetryOnTimeout) {
            // Se shouldRetryOnTimeout è true, consideriamo retryable qualsiasi WebClientRequestException, inclusi quelli causati da read timeout
            return throwable instanceof WebClientRequestException;
        }

        // Se shouldRetryOnTimeout è false, consideriamo retryable solo i WebClientRequestException che non sono causati da read timeout
        return throwable instanceof WebClientRequestException && (throwable.getCause() == null || !(throwable.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException));
    }

    // Override dei metodi per iniettare i valori specifici di timeout per le API di PEC di Infocamere
    @Override
    @Autowired
    public void setConnectionTimeoutMillis(@Value("${pn.national.registries.infocamere.pec.connection-timeout-millis}") int connectionTimeoutMillis) {
        super.setConnectionTimeoutMillis(connectionTimeoutMillis);
    }

    @Override
    @Autowired
    public void setReadTimeoutMillis(@Value("${pn.national.registries.infocamere.pec.read-timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }
}
