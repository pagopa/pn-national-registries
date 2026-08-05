package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomRetryConfigTest {

    private CustomRetryConfig customRetryConfig;

    @BeforeEach
    void setUp() {
        customRetryConfig = new CustomRetryConfig(3);
    }

    @Test
    void testBuildRetryExchangeFilterFunction() {
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientRequest clientRequest = mock(ClientRequest.class);
        ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));
        when(clientResponse.createException()).thenReturn(Mono.error(new RuntimeException("Test Exception")));

        ExchangeFilterFunction filterFunction = customRetryConfig.buildRetryExchangeFilterFunction();

        StepVerifier.create(filterFunction.filter(clientRequest, exchangeFunction))
                .expectError(RuntimeException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }

    @Test
    void buildRetryExchangeFilterFunction_withDefaultCondition() {
        ExchangeFilterFunction filterFunction = customRetryConfig.buildRetryExchangeFilterFunction();
        assertThat(filterFunction).isNotNull();
    }

    @Test
    void buildRetryExchangeFilterFunction_withCustomCondition() {
        Predicate<Throwable> customCondition = IOException.class::isInstance;
        ExchangeFilterFunction filterFunction = customRetryConfig.buildRetryExchangeFilterFunction(customCondition);
        assertThat(filterFunction).isNotNull();
    }

    @Test
    void buildRetryExchangeFilterFunction_withCustomConditionAndMaxAttempts() {
        Predicate<Throwable> customCondition = IOException.class::isInstance;
        ExchangeFilterFunction filterFunction = customRetryConfig.buildRetryExchangeFilterFunction(customCondition, 5);
        assertThat(filterFunction).isNotNull();
    }

    @Test
    void getRetryMaxAttempts_returnsConfiguredValue() {
        assertThat(customRetryConfig.getRetryMaxAttempts()).isEqualTo(3);
    }

    @Test
    void constructor_setsRetryMaxAttempts() {
        CustomRetryConfig config = new CustomRetryConfig(5);
        assertThat(config.getRetryMaxAttempts()).isEqualTo(5);
    }

    static Stream<Arguments> retryableExceptionsProvider() {
        HttpHeaders headers = new HttpHeaders();
        byte[] body = "error".getBytes(StandardCharsets.UTF_8);

        return Stream.of(
                Arguments.of(new SocketTimeoutException("Timeout occurred"), "TimeoutException"),
                Arguments.of(new SocketException("Socket error"), "SocketException"),
                Arguments.of(new SocketTimeoutException("Socket timeout"), "SocketTimeoutException"),
                Arguments.of(new SSLHandshakeException("SSL handshake failed"), "SSLHandshakeException"),
                Arguments.of(new UnknownHostException("Unknown host"), "UnknownHostException"),
                Arguments.of(new WebClientRequestException(new RuntimeException("error"), HttpMethod.GET, URI.create("http://test.com"), headers), "WebClientRequestException"),
                Arguments.of(WebClientResponseException.create(429, "Too Many Requests", headers, body, StandardCharsets.UTF_8), "TooManyRequests"),
                Arguments.of(WebClientResponseException.create(504, "Gateway Timeout", headers, body, StandardCharsets.UTF_8), "GatewayTimeout"),
                Arguments.of(WebClientResponseException.create(502, "Bad Gateway", headers, body, StandardCharsets.UTF_8), "BadGateway"),
                Arguments.of(WebClientResponseException.create(503, "Service Unavailable", headers, body, StandardCharsets.UTF_8), "ServiceUnavailable")
        );
    }

    @ParameterizedTest(name = "Should retry on {1}")
    @MethodSource("retryableExceptionsProvider")
    void testRetryCondition_ShouldRetryOnRetryableExceptions(Throwable exception, String exceptionName) {
        Predicate<Throwable> condition = CustomRetryConfig.defaultRetryCondition;
        assertTrue(condition.test(exception), "Expected to retry on " + exceptionName);
    }



    static Stream<Arguments> nonRetryableExceptionsProvider() {
        HttpHeaders headers = new HttpHeaders();
        byte[] body = "error".getBytes(StandardCharsets.UTF_8);

        return Stream.of(
                Arguments.of(new RuntimeException("Generic error"), "RuntimeException"),
                Arguments.of(new IllegalArgumentException("Invalid argument"), "IllegalArgumentException"),
                Arguments.of(WebClientResponseException.create(400, "Bad Request", headers, body, StandardCharsets.UTF_8), "BadRequest"),
                Arguments.of(WebClientResponseException.create(401, "Unauthorized", headers, body, StandardCharsets.UTF_8), "Unauthorized"),
                Arguments.of(WebClientResponseException.create(404, "Not Found", headers, body, StandardCharsets.UTF_8), "NotFound"),
                Arguments.of(WebClientResponseException.create(500, "Internal Server Error", headers, body, StandardCharsets.UTF_8), "InternalServerError")
        );
    }

    @ParameterizedTest(name = "Should not retry on {1}")
    @MethodSource("nonRetryableExceptionsProvider")
    void testRetryCondition_ShouldNotRetryOnNonRetryableExceptions(Throwable exception, String exceptionName) {
        Predicate<Throwable> condition = CustomRetryConfig.defaultRetryCondition;
        assertFalse(condition.test(exception), "Expected not to retry on " + exceptionName);
    }
}