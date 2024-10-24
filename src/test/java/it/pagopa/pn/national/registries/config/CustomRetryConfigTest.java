package it.pagopa.pn.national.registries.config;

import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomRetryConfigTest {

    private CustomRetryConfig customRetryConfig;

    @BeforeEach
    void setUp() {
        customRetryConfig = new CustomRetryConfig(3);
    }

    @Test
    void testIsRetryableException_TimeoutException() {
        Throwable exception = new ReadTimeoutException();
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_SocketException() {
        Throwable exception = new SocketException("Socket exception");
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_SocketTimeoutException() {
        Throwable exception = new SocketTimeoutException("Socket timeout exception");
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_SSLHandshakeException() {
        Throwable exception = new SSLHandshakeException("SSL handshake exception");
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_UnknownHostException() {
        Throwable exception = new UnknownHostException("Unknown host exception");
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_WebClientRequestException() {
        Throwable exception = new WebClientRequestException(new RuntimeException("WebClient request exception"), HttpMethod.GET, URI.create("http://localhost:8080/"), HttpHeaders.EMPTY);
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_TooManyRequests() {
        Throwable exception = WebClientResponseException.create(429, "Too Many Requests", null, null, null);
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_GatewayTimeout() {
        Throwable exception = WebClientResponseException.create(504, "Gateway Timeout", null, null, null);
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_BadGateway() {
        Throwable exception = WebClientResponseException.create(502, "Bad Gateway", null, null, null);
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_ServiceUnavailable() {
        Throwable exception = WebClientResponseException.create(503, "Service Unavailable", null, null, null);
        assertTrue(CustomRetryConfig.isRetryableException(exception));
    }

    @Test
    void testIsRetryableException_NonRetryableException() {
        Throwable exception = new IllegalArgumentException("Non-retryable exception");
        assertFalse(CustomRetryConfig.isRetryableException(exception));
    }
}