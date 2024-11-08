package it.pagopa.pn.national.registries.config;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.national.registries.utils.MaskTaxIdInPathUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Component
@Data
public class CustomRetryConfig {

    private final int retryMaxAttempts;

    public CustomRetryConfig(@Value("${pn.national.registries.custom.retry.max-attempts}") int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return (request, next) ->
                next.exchange(request).flatMap((clientResponse) ->
                                Mono.just(clientResponse).filter((response) ->
                                                clientResponse.statusCode().isError()).flatMap((response) ->
                                                clientResponse.createException())
                                        .flatMap(Mono::error)
                                        .thenReturn(clientResponse))
                        .retryWhen(Retry.backoff(this.retryMaxAttempts, Duration.ofMillis(25L))
                                .jitter(0.75)
                                .filter(this::isRetryableException)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                    Throwable lastExceptionInRetry = retrySignal.failure();
                                    log.warn("Retries exhausted {}, with last Exception: {}", retrySignal.totalRetries(), MaskTaxIdInPathUtils.maskTaxIdInPath(lastExceptionInRetry.getMessage()));
                                    return lastExceptionInRetry;
                                }));
    }

    private boolean isRetryableException(Throwable throwable) {
        boolean retryable = throwable instanceof TimeoutException ||
                throwable instanceof SocketException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof SSLHandshakeException ||
                throwable instanceof UnknownHostException ||
                throwable instanceof WebClientRequestException ||
                throwable instanceof WebClientResponseException.TooManyRequests ||
                throwable instanceof WebClientResponseException.GatewayTimeout ||
                throwable instanceof WebClientResponseException.BadGateway ||
                throwable instanceof WebClientResponseException.ServiceUnavailable
                ;
        if(retryable) {
            log.warn("Exception caught by retry: {}", MaskTaxIdInPathUtils.maskTaxIdInPath(Objects.requireNonNull(throwable.getMessage())));
        }
        return retryable;
    }
}
