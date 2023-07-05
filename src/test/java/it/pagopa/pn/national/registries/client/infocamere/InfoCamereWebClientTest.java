package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.national.registries.config.infocamere.InfoCamereWebClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class InfoCamereWebClientTest {

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        InfoCamereWebClientConfig infoCamereWebClientConfig = new InfoCamereWebClientConfig();
        infoCamereWebClientConfig.setTcpMaxPoolSize(1);
        infoCamereWebClientConfig.setTcpMaxQueuedConnections(1);
        infoCamereWebClientConfig.setTcpPoolIdleTimeout(1);
        infoCamereWebClientConfig.setTcpPendingAcquiredTimeout(1);
        InfoCamereWebClient infoCamereWebClient = new InfoCamereWebClient("basePath", true, infoCamereWebClientConfig);

        assertThrows(NullPointerException.class, infoCamereWebClient::init, "Input stream not contain valid certificates.");
    }

}
