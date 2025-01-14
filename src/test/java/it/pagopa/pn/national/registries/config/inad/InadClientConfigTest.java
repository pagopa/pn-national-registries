package it.pagopa.pn.national.registries.config.inad;

import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
class InadClientConfigTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private InadClientConfig inadClientConfig;


    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        inadClientConfig = new InadClientConfig(webClientBuilder,new CustomRetryConfig(1));
    }

    @Test
    void apiEstrazioniPuntualiApi_ShouldReturnValidApiInstance_WhenBasePathIsProvided() {
        ApiEstrazioniPuntualiApi result = inadClientConfig.apiEstrazioniPuntualiApi("http://example.com");
        assertNotNull(result, "Expected a non-null ApiEstrazioniPuntualiApi instance");
    }

    @Test
    void apiEstrazioniPuntualiApi_ShouldConfigureApiClientWithProvidedBasePath() {
        String expectedBasePath = "http://example.com";
        ApiEstrazioniPuntualiApi api = inadClientConfig.apiEstrazioniPuntualiApi(expectedBasePath);
        String actualBasePath = api.getApiClient().getBasePath();
        assertNotNull(actualBasePath, "Base path should not be null");
        assertEquals(expectedBasePath, actualBasePath, "Base path should match the provided value");
    }

    @Test
    void testBuildRetryExchangeFilterFunction() {
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientRequest clientRequest = mock(ClientRequest.class);
        ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));
        when(clientResponse.createException()).thenReturn(Mono.error(new RuntimeException("Test Exception")));

        ExchangeFilterFunction filterFunction = inadClientConfig.buildRetryExchangeFilterFunction();

        StepVerifier.create(filterFunction.filter(clientRequest, exchangeFunction))
                .expectError(RuntimeException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }
}