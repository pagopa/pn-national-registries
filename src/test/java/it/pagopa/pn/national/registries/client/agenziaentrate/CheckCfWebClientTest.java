package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckCfWebClientTest {

    @InjectMocks
    CheckCfWebClient checkCfWebclient;
    
    @Mock
    CheckCfSecretConfig checkCfSecretConfig;

    @Test
    void testInit() {
        CheckCfWebClientConfig checkCfWebClientConfig = new CheckCfWebClientConfig();
        CheckCfWebClient checkCfWebClient = new CheckCfWebClient(true, "test.it", checkCfSecretConfig, checkCfWebClientConfig);
        Assertions.assertThrows(NullPointerException.class, checkCfWebClient::init);
    }

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        when(checkCfSecretConfig.getCheckCfAuthChannelSecret()).thenReturn(sslData);

        assertThrows(IllegalArgumentException.class, () -> checkCfWebclient.buildSslContext(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        CheckCfWebClientConfig webClientConfig = new CheckCfWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(true, "", checkCfSecretConfig, webClientConfig);
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        sslData.setPub("pub");
        sslData.setTrust("trust");
        when(checkCfSecretConfig.getCheckCfAuthChannelSecret()).thenReturn(sslData);

        assertThrows(IllegalArgumentException.class, checkCfWebclient::init, "Input stream not contain valid certificates.");
    }

}
