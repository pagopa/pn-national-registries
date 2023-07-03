package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {InfoCamereTokenClient.class, String.class})
@ExtendWith(SpringExtension.class)
class InfoCamereTokenClientTest {
    @Autowired
    private InfoCamereTokenClient infoCamereTokenClient;

    @MockBean
    private InfoCamereWebClient infoCamereGetTokenWebClient;

    @MockBean
    private InfoCamereJwsGenerator infoCamereJwsGenerator;
    @Mock
    WebClient webClient;

    final String clientId = "tezt_clientId";

    private static WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private static WebClient.RequestBodySpec requestBodySpec;
    private static WebClient.RequestHeadersSpec requestHeadersSpec;
    private static WebClient.ResponseSpec responseSpec;
    private static WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @BeforeAll
    static void setup() {
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    }
    /**
     * Method under test: {@link InfoCamereTokenClient#getToken(String)}
     */
    @Test
    void testGetToken2() throws UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        when(infoCamereJwsGenerator.createAuthRest(any()))
                .thenThrow(new WebClientResponseException(1, "Status Text", headers, "AAAAAAAA".getBytes(StandardCharsets.UTF_8), null));
        assertThrows(WebClientResponseException.class, () -> infoCamereTokenClient.getToken("Scope"));
        verify(infoCamereJwsGenerator).createAuthRest(any());
    }

    @Test
    void callGetTokenTest() {
        when(infoCamereGetTokenWebClient.init()).thenReturn(webClient);
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(infoCamereGetTokenWebClient, clientId, infoCamereJwsGenerator);

        String scope = "test_scope";
        String jws = "jws";

        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope)).expectNext(jws).verifyComplete();
    }

    @Test
    void testGetTokenWebException() {
        String scope = "test_scope";
        when(infoCamereGetTokenWebClient.init()).thenReturn(webClient);
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(infoCamereGetTokenWebClient, clientId, infoCamereJwsGenerator);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope)).expectError(PnNationalRegistriesException.class).verify();
    }

    @Test
    void testGetTokenWebUnauthorizedException() {
        String scope = "test_scope";
        when(infoCamereGetTokenWebClient.init()).thenReturn(webClient);
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(infoCamereGetTokenWebClient, clientId, infoCamereJwsGenerator);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope)).expectError(PnInternalException.class).verify();
    }
}

