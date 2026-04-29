package it.pagopa.pn.national.registries.config.infocamere;

import io.netty.handler.timeout.ReadTimeoutException;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.PecApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniPecClientConfigTest {

    private static final String BASE_PATH = "basePath";

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;
    @Mock
    private CustomRetryConfig customRetryConfig;

    NationalRegistriesConfig.InfoCamere infoCamere;

    private IniPecClientConfig iniPecClientConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        infoCamere = new NationalRegistriesConfig.InfoCamere();
        infoCamere.setBaseUrl(BASE_PATH);
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setMaxRetryAttempts(3);
        inipec.setRetryOnTimeout(true);
        infoCamere.setInipec(inipec);
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamere);
        iniPecClientConfig = new IniPecClientConfig(customRetryConfig, webClientBuilder, nationalRegistriesConfig);
    }

    @Test
    void testPecApi() {
        PecApi pecApi = iniPecClientConfig.pecApi();
        assertEquals(BASE_PATH, pecApi.getApiClient().getBasePath());
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
        assertTrue(iniPecClientConfig.retryCondition(exception), "Expected to retry on " + exceptionName);
    }

    @ParameterizedTest(name = "Should not retry on {1}")
    @MethodSource("nonRetryableExceptionsProvider")
    void testRetryCondition_ShouldNotRetryOnNonRetryableExceptions(Throwable exception, String exceptionName) {
        assertFalse(iniPecClientConfig.retryCondition(exception), "Expected not to retry on " + exceptionName);
    }

    @Test
    void testRetryCondition_ShouldNotRetryOnTimeoutWhenShouldRetryOnTimeoutIsFalse() {
        infoCamere.getInipec().setRetryOnTimeout(false);


        HttpHeaders headers = new HttpHeaders();
        WebClientRequestException ex = new WebClientRequestException(
                ReadTimeoutException.INSTANCE, HttpMethod.GET, URI.create("http://localhost"), headers);

        assertFalse(iniPecClientConfig.retryCondition(ex), "Expected not to retry on WebClientRequestException caused by ReadTimeoutException when shouldRetryOnTimeout is false");
    }

    @Test
    void testRetryCondition_ShouldRetryOnWebClientRequestExceptionWhenShouldRetryOnTimeoutIsTrue() {
        HttpHeaders headers = new HttpHeaders();
        WebClientRequestException ex = new WebClientRequestException(
                ReadTimeoutException.INSTANCE, HttpMethod.GET, URI.create("http://localhost"), headers);

        assertTrue(iniPecClientConfig.retryCondition(ex), "Expected to retry on WebClientRequestException caused by ReadTimeoutException when shouldRetryOnTimeout is true");
    }


}