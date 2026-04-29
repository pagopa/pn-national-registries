package it.pagopa.pn.national.registries.config.infocamere;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
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
public class IniPecClientConfig extends CommonBaseClient {
    private final CustomRetryConfig customRetryConfig;
    private final WebClient pecWebClient;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    public IniPecClientConfig(
            CustomRetryConfig customRetryConfig,
            WebClient.Builder builder,
            NationalRegistriesConfig nationalRegistriesConfig
    ) {
        this.nationalRegistriesConfig = nationalRegistriesConfig;
        this.customRetryConfig = customRetryConfig;
        this.pecWebClient = initWebClient(builder);
    }

    @Bean
    PecApi pecApi() {
        var apiClient = new ApiClient(pecWebClient);
        apiClient.setBasePath(nationalRegistriesConfig.getInfoCamere().getBaseUrl());
        return new PecApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction(this::retryCondition, nationalRegistriesConfig.getInfoCamere().getInipec().getMaxRetryAttempts());
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
        if(nationalRegistriesConfig.getInfoCamere().getInipec().isRetryOnTimeout()) {
            // Se shouldRetryOnTimeout è true, consideriamo retryable qualsiasi WebClientRequestException, inclusi quelli causati da read timeout
            return throwable instanceof WebClientRequestException;
        }

        // Se shouldRetryOnTimeout è false, consideriamo retryable solo i WebClientRequestException che non sono causati da read timeout
        return throwable instanceof WebClientRequestException && !(throwable.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException);
    }

    // Override dei metodi per iniettare i valori specifici di timeout per le API di PEC di Infocamere
    @Override
    @Autowired
    public void setConnectionTimeoutMillis(@Value("${pn.national.registries.inipec.connection-timeout-millis}") int connectionTimeoutMillis) {
        super.setConnectionTimeoutMillis(connectionTimeoutMillis);
    }

    @Override
    @Autowired
    public void setReadTimeoutMillis(@Value("${pn.national.registries.inipec.read-timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }
}
