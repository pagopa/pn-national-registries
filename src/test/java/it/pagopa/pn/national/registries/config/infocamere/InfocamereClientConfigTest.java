package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
class InfocamereClientConfigTest {
    private static final String BASE_PATH = "basePath";

    /**
     * Method under test: {@link InfocamereClientConfig#authenticationApi(String)}
     */
    @Test
    void testAuthenticationApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));

        AuthenticationApi authenticationApi = infocamereClientConfig.authenticationApi(BASE_PATH);
        assertEquals(BASE_PATH, authenticationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentationApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));

        LegalRepresentationApi legalRepresentationApi = infocamereClientConfig.legalRepresentationApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentativeApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));

        LegalRepresentativeApi legalRepresentativeApi = infocamereClientConfig.legalRepresentativeApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentativeApi.getApiClient().getBasePath());
    }

    @Test
    void testPecApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));

        PecApi pecApi = infocamereClientConfig.pecApi(BASE_PATH);
        assertEquals(BASE_PATH, pecApi.getApiClient().getBasePath());
    }

    @Test
    void testSedeApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));

        SedeApi sedeApi = infocamereClientConfig.sedeApi(BASE_PATH);
        assertEquals(BASE_PATH, sedeApi.getApiClient().getBasePath());
    }

    @Test
    void testBuildRetryExchangeFilterFunction() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig(new CustomRetryConfig(3));
        CustomRetryConfig customRetryConfig = mock(CustomRetryConfig.class);
        when(customRetryConfig.getRetryMaxAttempts()).thenReturn(1);
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientRequest clientRequest = mock(ClientRequest.class);
        ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));
        when(clientResponse.createException()).thenReturn(Mono.error(new RuntimeException("Test Exception")));

        ExchangeFilterFunction filterFunction = infocamereClientConfig.buildRetryExchangeFilterFunction();

        StepVerifier.create(filterFunction.filter(clientRequest, exchangeFunction))
                .expectError(RuntimeException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }
}
