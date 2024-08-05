package it.pagopa.pn.national.registries.config.ipa;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.api.IpaApi;
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
class IpaClientConfigTest {
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    private IpaClientConfig ipaClientConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        ipaClientConfig = new IpaClientConfig();
    }

    @Test
    void ipaApi() {
        IpaApi ipaApi = ipaClientConfig.ipaApi("basePath");

        assertNotNull(ipaApi);
        assertEquals("basePath", ipaApi.getApiClient().getBasePath());
    }
}