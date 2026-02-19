package it.pagopa.pn.national.registries.config.infocamere;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import it.pagopa.pn.national.registries.utils.MaskTaxIdInPathUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;

@Configuration
@Slf4j
public class InfocamereClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;
    private final WebClient infocamereWebClient;

    public InfocamereClientConfig(CustomRetryConfig customRetryConfig, WebClient.Builder builder) {
        this.customRetryConfig = customRetryConfig;
        this.infocamereWebClient = initWebClient(builder);
    }

    @Bean
    AuthenticationApi authenticationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(infocamereWebClient);
        apiClient.setBasePath(basePath);
        return new AuthenticationApi(apiClient);
    }


    @Bean
    LegalRepresentationApi legalRepresentationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(infocamereWebClient);
        apiClient.setBasePath(basePath);
        return new LegalRepresentationApi(apiClient);
    }

    @Bean
    PecApi pecApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(infocamereWebClient);
        apiClient.setBasePath(basePath);
        return new PecApi(apiClient);
    }

    @Bean
    SedeApi sedeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(infocamereWebClient);
        apiClient.setBasePath(basePath);
        return new SedeApi(apiClient);
    }

    @Bean
    LegalRepresentativeApi legalRepresentativeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(infocamereWebClient);
        apiClient.setBasePath(basePath);
        return new LegalRepresentativeApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return customRetryConfig.buildRetryExchangeFilterFunction(this::retryCondition);
    }

    private boolean retryCondition(Throwable throwable) {
        boolean retryable = throwable instanceof TimeoutException ||
                throwable instanceof SocketException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof SSLHandshakeException ||
                throwable instanceof UnknownHostException ||
                throwable instanceof WebClientRequestException ||
                throwable instanceof WebClientResponseException.TooManyRequests ||
                throwable instanceof WebClientResponseException.GatewayTimeout ||
                throwable instanceof WebClientResponseException.BadGateway ||
                throwable instanceof WebClientResponseException.InternalServerError ||
                throwable instanceof WebClientResponseException.ServiceUnavailable
                ;
        if(retryable) {
            String maskedErrorMessage = Optional.ofNullable(throwable.getMessage())
                    .map(MaskTaxIdInPathUtils::maskTaxIdInPath)
                    .orElse("Unknown error");
            log.warn("Exception {} caught by retry: {}", throwable.getClass().getName(), maskedErrorMessage);
        }
        return retryable;
    }
}
