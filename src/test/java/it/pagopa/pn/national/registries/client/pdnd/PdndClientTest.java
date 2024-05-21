package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PdndClient.class})
@ExtendWith(SpringExtension.class)
class PdndClientTest {
    @MockBean
    AuthApi authApi;

    @Test
    void testCallCreateToken() {
        PdndClient pdndClient = new PdndClient(authApi);

        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken("accessToken");
        clientCredentialsResponse.setTokenType(TokenType.BEARER);
        clientCredentialsResponse.setExpiresIn(600);

        when(authApi.createToken(any(), any(), any(), any())).thenReturn(Mono.just(clientCredentialsResponse));

        StepVerifier.create(pdndClient.createToken("", "", "", ""))
                .expectNext(clientCredentialsResponse)
                .verifyComplete();
    }

    @Test
    void testCallCreateTokenDoOnError() {
        PdndClient pdndClient = new PdndClient(authApi);

        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        WebClientResponseException webClientResponseException = new WebClientResponseException(test, HttpStatus.NOT_FOUND.value(), test, headers, testByteArray, Charset.defaultCharset());
        when(authApi.createToken(any(), any(), any(), any())).thenReturn(Mono.error(webClientResponseException));
        StepVerifier.create(pdndClient.createToken("test", "test", "test", "test"))
                .verifyError(WebClientResponseException.class);
    }

}
