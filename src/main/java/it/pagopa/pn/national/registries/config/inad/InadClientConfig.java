package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.ApiClient;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
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
public class InadClientConfig extends CommonBaseClient {

    private final CustomRetryConfig customRetryConfig;

    @Bean
    ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi(@Value("${pn.national.registries.inad.base-path}") String basePath) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(basePath);
        return new ApiEstrazioniPuntualiApi(apiClient);
    }

    @Override
    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return (request, next) -> {
            return next.exchange(request).flatMap((clientResponse) -> {
                return Mono.just(clientResponse).filter((response) -> {
                    return clientResponse.statusCode().isError();
                }).flatMap((response) -> {
                    return clientResponse.createException();
                }).flatMap(Mono::error).thenReturn(clientResponse);
            }).retryWhen(Retry.backoff((long) customRetryConfig.getRetryMaxAttempts(), Duration.ofMillis(25L)).jitter(0.75).filter(throwable -> CustomRetryConfig.isRetryableException(throwable, MaskTaxIdInPathUtils.maskTaxIdInPathInad(throwable.getMessage()))).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                Throwable lastExceptionInRetry = retrySignal.failure();
                log.warn("Retries exhausted {}, with last Exception: {}", retrySignal.totalRetries(), MaskTaxIdInPathUtils.maskTaxIdInPathInad(lastExceptionInRetry.getMessage()));
                return lastExceptionInRetry;
            }));
        };
    }
}
