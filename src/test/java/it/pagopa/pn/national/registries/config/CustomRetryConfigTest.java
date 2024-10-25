package it.pagopa.pn.national.registries.config;

import io.netty.handler.timeout.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomRetryConfigTest {

    private CustomRetryConfig customRetryConfig;
    private Method isRetryableExceptionMethod;

    @BeforeEach
    void setUp() throws Exception {
        customRetryConfig = new CustomRetryConfig(3);
        isRetryableExceptionMethod = CustomRetryConfig.class.getDeclaredMethod("isRetryableException", Throwable.class);
        isRetryableExceptionMethod.setAccessible(true);
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
    void testIsRetryableException_TimeoutException() throws Exception {
        // Create a mock TimeoutException using Mockito
        TimeoutException exception = mock(TimeoutException.class);
        when(exception.getMessage()).thenReturn("Timeout exception");
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_SocketException() throws Exception {
        Throwable exception = new SocketException("Socket exception");
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_SocketTimeoutException() throws Exception {
        Throwable exception = new SocketTimeoutException("Socket timeout exception");
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_SSLHandshakeException() throws Exception {
        Throwable exception = new SSLHandshakeException("SSL handshake exception");
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_UnknownHostException() throws Exception {
        Throwable exception = new UnknownHostException("Unknown host exception");
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_WebClientRequestException() throws Exception {
        Throwable exception = new WebClientRequestException(new RuntimeException("WebClient request exception"), HttpMethod.GET, URI.create("http://localhost:8080/"), HttpHeaders.EMPTY);
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_TooManyRequests() throws Exception {
        Throwable exception = WebClientResponseException.create(429, "Too Many Requests", null, null, null);
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_GatewayTimeout() throws Exception {
        Throwable exception = WebClientResponseException.create(504, "Gateway Timeout", null, null, null);
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_BadGateway() throws Exception {
        Throwable exception = WebClientResponseException.create(502, "Bad Gateway", null, null, null);
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_ServiceUnavailable() throws Exception {
        Throwable exception = WebClientResponseException.create(503, "Service Unavailable", null, null, null);
        assertTrue((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }

    @Test
    void testIsRetryableException_NonRetryableException() throws Exception {
        Throwable exception = new IllegalArgumentException("Non-retryable exception");
        assertFalse((Boolean) isRetryableExceptionMethod.invoke(customRetryConfig, exception));
    }
}