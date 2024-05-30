package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AdELegalWebClientTest {
    @MockBean
    AdeLegalSecretConfig adeLegalSecretConfig;
    @MockBean
    SecureWebClientUtils secureWebClientUtils;
    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;


    @Test
    void testInit() throws SSLException {
        AdELegalWebClient adELegalWebClient = new AdELegalWebClient("basePath", adeLegalSecretConfig, secureWebClientUtils, pnNationalRegistriesSecretService);

        TrustData trustData = mock(TrustData.class);

        when(adeLegalSecretConfig.getTrustData()).thenReturn("secret");
        when(pnNationalRegistriesSecretService.getTrustedCertFromSecret(anyString())).thenReturn(trustData);
        when(secureWebClientUtils.getSslContext(any(), any())).thenReturn(mock(SslContext.class));

        WebClient webClient1 = adELegalWebClient.init();
        assertNotNull(webClient1);
    }

    @Test
    void testInitException() throws SSLException {
        AdELegalWebClient adELegalWebClient = new AdELegalWebClient("basePath", adeLegalSecretConfig, secureWebClientUtils, pnNationalRegistriesSecretService);

        TrustData trustData = mock(TrustData.class);

        when(adeLegalSecretConfig.getTrustData()).thenReturn("secret");
        when(pnNationalRegistriesSecretService.getTrustedCertFromSecret(anyString())).thenReturn(trustData);
        when(secureWebClientUtils.getSslContext(any(), any())).thenThrow(SSLException.class);

        assertThrows(PnInternalException.class, adELegalWebClient::init);
    }
}
