package it.pagopa.pn.national.registries.config.adecheckcf;

import io.netty.handler.ssl.SslContext;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.CustomRetryConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CheckCfClientConfigtest {

    @MockBean
    static SecureWebClientUtils secureWebClientUtils;
    @MockBean
    static CheckCfSecretConfig checkCfSecretConfig;
    @MockBean
    static PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private CustomRetryConfig customRetryConfig;

    @Mock
    private WebClient webClient;

    private CheckCfClientConfig checkCfClientConfig;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filters(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        checkCfClientConfig = new CheckCfClientConfig(customRetryConfig, pnNationalRegistriesSecretService, checkCfSecretConfig, secureWebClientUtils);
    }

    @Test
    void e002ServiceApi() throws SSLException {
        TrustData trustData = new TrustData();
        trustData.setTrust("trust");

        when(pnNationalRegistriesSecretService.getTrustedCertFromSecret(any())).thenReturn(trustData);
        when(secureWebClientUtils.getSslContextForAde(any(), any())).thenReturn(mock(SslContext.class));
        when(customRetryConfig.buildRetryExchangeFilterFunction()).thenReturn(mock(ExchangeFilterFunction.class));
        VerificheApi verificheapi = checkCfClientConfig.verificheApi("basePath");

        assertNotNull(verificheapi);
        assertEquals("basePath", verificheapi.getApiClient().getBasePath());
    }
}
