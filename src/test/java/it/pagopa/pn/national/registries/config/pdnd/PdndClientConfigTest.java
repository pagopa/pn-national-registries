package it.pagopa.pn.national.registries.config.pdnd;

import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class PdndClientConfigTest {
    private PdndClientConfig pdndClientConfig;

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        pdndClientConfig = new PdndClientConfig(webClientBuilder);
    }

    @Test
    void authApi() {
        AuthApi authApi = pdndClientConfig.authApi("basePath");
        assertNotNull(authApi);
        assertEquals("basePath", authApi.getApiClient().getBasePath());
    }
}