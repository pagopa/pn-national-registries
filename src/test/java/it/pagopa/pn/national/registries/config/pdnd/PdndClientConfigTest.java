package it.pagopa.pn.national.registries.config.pdnd;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdndClientConfigTest {

    @InjectMocks
    private PdndClientConfig pdndClientConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        NationalRegistriesConfig.Pdnd pdndConfig = new NationalRegistriesConfig.Pdnd();
        pdndConfig.setBaseUrl("basePath");
        when(nationalRegistriesConfig.getPdnd()).thenReturn(pdndConfig);
    }

    @Test
    void authApi() {
        AuthApi authApi = pdndClientConfig.authApi();
        assertNotNull(authApi);
        assertEquals("basePath", authApi.getApiClient().getBasePath());
    }
}