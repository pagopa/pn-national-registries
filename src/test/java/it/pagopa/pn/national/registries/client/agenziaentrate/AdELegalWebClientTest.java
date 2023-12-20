package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.client.SecureWebClient;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalWebClientConfig;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(classes = {AdELegalWebClient.class, SecureWebClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class AdELegalWebClientTest {

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    @Mock
    AdeLegalSecretConfig adeLegalSecretConfig;

    @Test
    void testInit() {
        AdeLegalWebClientConfig adeLegalWebClientConfig = new AdeLegalWebClientConfig();
        adeLegalWebClientConfig.setTcpMaxPoolSize(1);
        adeLegalWebClientConfig.setTcpMaxQueuedConnections(1);
        adeLegalWebClientConfig.setTcpPoolIdleTimeout(1);
        adeLegalWebClientConfig.setTcpPendingAcquiredTimeout(1);
        AdELegalWebClient adELegalWebClient = new AdELegalWebClient( "test.it", adeLegalSecretConfig, pnNationalRegistriesSecretService);
        assertThrows(NullPointerException.class, adELegalWebClient::init);
    }

}
