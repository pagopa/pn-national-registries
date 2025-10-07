package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class InfocamereClientConfigTest {
    private static final String BASE_PATH = "basePath";

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private CustomRetryConfig customRetryConfig;

    private InfocamereClientConfig infocamereClientConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        infocamereClientConfig = new InfocamereClientConfig(customRetryConfig, webClientBuilder);
    }
    /**
     * Method under test: {@link InfocamereClientConfig#authenticationApi(String)}
     */
    @Test
    void testAuthenticationApi() {
        AuthenticationApi authenticationApi = infocamereClientConfig.authenticationApi(BASE_PATH);
        assertEquals(BASE_PATH, authenticationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentationApi() {
        LegalRepresentationApi legalRepresentationApi = infocamereClientConfig.legalRepresentationApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentativeApi() {
        LegalRepresentativeApi legalRepresentativeApi = infocamereClientConfig.legalRepresentativeApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentativeApi.getApiClient().getBasePath());
    }

    @Test
    void testPecApi() {
        PecApi pecApi = infocamereClientConfig.pecApi(BASE_PATH);
        assertEquals(BASE_PATH, pecApi.getApiClient().getBasePath());
    }

    @Test
    void testSedeApi() {
        SedeApi sedeApi = infocamereClientConfig.sedeApi(BASE_PATH);
        assertEquals(BASE_PATH, sedeApi.getApiClient().getBasePath());
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
                Arguments.of(new WebClientRequestException(new RuntimeException(), HttpMethod.GET, URI.create("http://test.com"), headers), "WebClientRequestException"),
                Arguments.of(WebClientResponseException.create(429, "Too Many Requests", headers, body, StandardCharsets.UTF_8), "TooManyRequests"),
                Arguments.of(WebClientResponseException.create(504, "Gateway Timeout", headers, body, StandardCharsets.UTF_8), "GatewayTimeout"),
                Arguments.of(WebClientResponseException.create(502, "Bad Gateway", headers, body, StandardCharsets.UTF_8), "BadGateway"),
                Arguments.of(WebClientResponseException.create(500, "Internal Server Error", headers, body, StandardCharsets.UTF_8), "InternalServerError"),
                Arguments.of(WebClientResponseException.create(503, "Service Unavailable", headers, body, StandardCharsets.UTF_8), "ServiceUnavailable")
        );
    }

    static Stream<Arguments> nonRetryableExceptionsProvider() {
        HttpHeaders headers = new HttpHeaders();
        byte[] body = "error".getBytes(StandardCharsets.UTF_8);

        return Stream.of(
                Arguments.of(new RuntimeException("Generic error"), "RuntimeException"),
                Arguments.of(new IllegalArgumentException("Invalid argument"), "IllegalArgumentException"),
                Arguments.of(WebClientResponseException.create(400, "Bad Request", headers, body, StandardCharsets.UTF_8), "BadRequest"),
                Arguments.of(WebClientResponseException.create(401, "Unauthorized", headers, body, StandardCharsets.UTF_8), "Unauthorized"),
                Arguments.of(WebClientResponseException.create(404, "Not Found", headers, body, StandardCharsets.UTF_8), "NotFound")
        );
    }

    @ParameterizedTest(name = "Should retry on {1}")
    @MethodSource("retryableExceptionsProvider")
    void testRetryCondition_ShouldRetryOnRetryableExceptions(Throwable exception, String exceptionName) {
        // Arrange
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientRequest clientRequest = mock(ClientRequest.class);

        when(customRetryConfig.buildRetryExchangeFilterFunction(any()))
                .thenAnswer(invocation -> {
                    Predicate<Throwable> retryPredicate = invocation.getArgument(0);
                    return (ExchangeFilterFunction) (request, next) ->
                            next.exchange(request).onErrorResume(error -> {
                                if (retryPredicate.test(error)) {
                                    return next.exchange(request);
                                }
                                return Mono.error(error);
                            });
                });

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.error(exception));

        // Act
        ExchangeFilterFunction filterFunction = infocamereClientConfig.buildRetryExchangeFilterFunction();

        // Assert
        StepVerifier.create(filterFunction.filter(clientRequest, exchangeFunction))
                .expectError(exception.getClass())
                .verify();

        // Verifica che sia stato chiamato almeno 2 volte (tentativo iniziale + retry)
        verify(exchangeFunction, atLeast(2)).exchange(any(ClientRequest.class));
    }

    @ParameterizedTest(name = "Should NOT retry on {1}")
    @MethodSource("nonRetryableExceptionsProvider")
    void testRetryCondition_ShouldNotRetryOnNonRetryableExceptions(Throwable exception, String exceptionName) {
        // Arrange
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientRequest clientRequest = mock(ClientRequest.class);

        when(customRetryConfig.buildRetryExchangeFilterFunction(any()))
                .thenAnswer(invocation -> {
                    Predicate<Throwable> retryPredicate = invocation.getArgument(0);
                    return (ExchangeFilterFunction) (request, next) ->
                            next.exchange(request).onErrorResume(error -> {
                                if (retryPredicate.test(error)) {
                                    return next.exchange(request);
                                }
                                return Mono.error(error);
                            });
                });

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.error(exception));

        // Act
        ExchangeFilterFunction filterFunction = infocamereClientConfig.buildRetryExchangeFilterFunction();

        // Assert
        StepVerifier.create(filterFunction.filter(clientRequest, exchangeFunction))
                .expectError(exception.getClass())
                .verify();

        // Verifica che sia stato chiamato solo 1 volta (nessun retry)
        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }
}
