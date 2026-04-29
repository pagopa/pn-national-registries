package it.pagopa.pn.national.registries.config.anpr;

import io.netty.handler.ssl.SslContext;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.config.pdnd.PdndClientConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class AnprClientConfigTest {

    @Mock
    SecureWebClientUtils secureWebClientUtils;

    @InjectMocks
    private AnprClientConfig anprClientConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        NationalRegistriesConfig.Anpr anpr = new NationalRegistriesConfig.Anpr();
        anpr.setBaseUrl("basePath");
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);
    }


    @Test
    void e002ServiceApi() throws SSLException {
        TrustData trustData = new TrustData();
        trustData.setTrust("trust");

        when(pnNationalRegistriesSecretService.getTrustedCertFromSecret(any())).thenReturn(trustData);
        when(secureWebClientUtils.getSslContext(any(), anyString())).thenReturn(mock(SslContext.class));
        E002ServiceApi e002ServiceApi = anprClientConfig.e002ServiceApi();

        assertNotNull(e002ServiceApi);
        assertEquals("basePath", e002ServiceApi.getApiClient().getBasePath());
    }
}