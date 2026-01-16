package it.pagopa.pn.national.registries.config.anpr;

import io.netty.handler.ssl.SslContext;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AnprClientConfigTest {

    @MockitoBean
    SecureWebClientUtils secureWebClientUtils;
    @MockitoBean
    AnprSecretConfig anprSecretConfig;
    @MockitoBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;

    private AnprClientConfig anprClientConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        anprClientConfig = new AnprClientConfig(secureWebClientUtils, anprSecretConfig, pnNationalRegistriesSecretService,webClientBuilder);
    }

    @Test
    void e002ServiceApi() throws SSLException {
        TrustData trustData = new TrustData();
        trustData.setTrust("trust");

        when(pnNationalRegistriesSecretService.getTrustedCertFromSecret(any())).thenReturn(trustData);
        when(secureWebClientUtils.getSslContext(any(), anyString())).thenReturn(mock(SslContext.class));
        E002ServiceApi e002ServiceApi = anprClientConfig.e002ServiceApi("basePath");

        assertNotNull(e002ServiceApi);
        assertEquals("basePath", e002ServiceApi.getApiClient().getBasePath());
    }
}