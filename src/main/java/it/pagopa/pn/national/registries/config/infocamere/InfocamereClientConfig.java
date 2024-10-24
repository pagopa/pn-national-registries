package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import it.pagopa.pn.national.registries.utils.MaskTaxIdInPathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InfocamereClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;

    @Bean
    AuthenticationApi authenticationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new AuthenticationApi(apiClient);
    }


    @Bean
    LegalRepresentationApi legalRepresentationApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new LegalRepresentationApi(apiClient);
    }

    @Bean
    LegalRepresentativeApi legalRepresentativeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new LegalRepresentativeApi(apiClient);
    }

    @Bean
    PecApi pecApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new PecApi(apiClient);
    }

    @Bean
    SedeApi sedeApi(@Value("${pn.national.registries.infocamere.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new SedeApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return (request, next) ->
                next.exchange(request).flatMap((clientResponse) ->
                                Mono.just(clientResponse).filter((response) ->
                                                clientResponse.statusCode().isError()).flatMap((response) ->
                                                clientResponse.createException())
                                        .flatMap(Mono::error)
                                        .thenReturn(clientResponse))
                        .retryWhen(Retry.backoff(customRetryConfig.getRetryMaxAttempts(), Duration.ofMillis(25L))
                                .jitter(0.75)
                                .filter(CustomRetryConfig::isRetryableException)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                    Throwable lastExceptionInRetry = retrySignal.failure();
                                    log.warn("Retries exhausted {}, with last Exception: {}", retrySignal.totalRetries(), MaskTaxIdInPathUtils.maskTaxIdInPathICRegistroImprese(lastExceptionInRetry.getMessage()));
                                    return lastExceptionInRetry;
                                }));
    }
}
