package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.config.anpr.AnprWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprWebClientTest {

    @InjectMocks
    AnprWebClient anprWebClient;

    @Mock
    AnprSecretConfig anprSecretConfig;

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        when(anprSecretConfig.getAnprAuthChannelSecret()).thenReturn(sslData);
        assertThrows(IllegalArgumentException.class, () -> anprWebClient.buildSslContext(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        AnprWebClientConfig webClientConfig = new AnprWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        AnprWebClient anprWebClient = new AnprWebClient(true, "", anprSecretConfig, webClientConfig);
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        sslData.setPub("pub");
        sslData.setTrust("trust");
        when(anprSecretConfig.getAnprAuthChannelSecret()).thenReturn(sslData);

        assertThrows(IllegalArgumentException.class, anprWebClient::init, "Input stream not contain valid certificates.");
    }
}
