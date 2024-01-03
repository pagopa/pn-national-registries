package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.config.anpr.AnprWebClientConfig;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AnprWebClientTest {

    @InjectMocks
    AnprWebClient anprWebClient;

    @Mock
    AnprSecretConfig anprSecretConfig;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        AnprWebClientConfig webClientConfig = new AnprWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        AnprWebClient anprWebClient = new AnprWebClient( "",  anprSecretConfig, pnNationalRegistriesSecretService);

        assertThrows(NullPointerException.class, anprWebClient::init, "Input stream not contain valid certificates.");
    }
}
