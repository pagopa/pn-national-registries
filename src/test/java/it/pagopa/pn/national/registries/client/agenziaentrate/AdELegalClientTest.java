package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.utils.XMLWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {AdELegalClient.class})
@ExtendWith(SpringExtension.class)
class AdELegalClientTest {

    @Autowired
    private AdELegalClient adELegalClient;

    @MockBean
    WebClient webClient;

    @MockBean
    AdELegalWebClient adELegalWebClient;

    @MockBean
    XMLWriter xMLWriter;

    @MockBean
    AdeLegalSecretConfig adeLegalSecretConfig;

    @Test
    void checkTaxIdAndVatNumberErrorTest() {
        when(adELegalWebClient.init()).thenReturn(webClient);
        AdELegalClient adELegalClient = new AdELegalClient(adELegalWebClient, xMLWriter);

        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        WebClientResponseException webClientResponseException = new WebClientResponseException(test, 500, test, headers, testByteArray, Charset.defaultCharset());

        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyFilterDto.setTaxId("setTaxId");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(xMLWriter.getEnvelope(any(), any())).thenReturn("test");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/SPCBooleanoRappWS/VerificaRappresentanteEnteService")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(adELegalClient.checkTaxIdAndVatNumberAdE(adeLegalRequestBodyFilterDto))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        AdELegalClient adELegalClient = new AdELegalClient(adELegalWebClient, xMLWriter);
        assertFalse(adELegalClient.shouldRetry(new Exception()));
    }

    @Test
    @DisplayName("Should return true when the exception is webclientresponseexception and the status code is 401")
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        AdELegalClient adELegalClient = new AdELegalClient(adELegalWebClient, xMLWriter);

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(adELegalClient.shouldRetry(webClientResponseException));
    }

    /**
     * Method under test: {@link AdELegalClient#shouldRetry(Throwable)}
     */
    @Test
    void testShouldRetry() {
        assertFalse(adELegalClient.shouldRetry(new Throwable()));
    }
}
