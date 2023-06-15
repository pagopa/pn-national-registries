package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfWebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CheckCfWebClientTest {

    @InjectMocks
    CheckCfWebClient checkCfWebclient;
    
    @Mock
    CheckCfSecretConfig checkCfSecretConfig;

    @Mock
    SsmParameterConsumerActivation ssmParameterConsumerActivation;

    @Test
    void testInit() {
        CheckCfWebClientConfig checkCfWebClientConfig = new CheckCfWebClientConfig();
        CheckCfWebClient checkCfWebClient = new CheckCfWebClient(true, "test.it", "", checkCfWebClientConfig, ssmParameterConsumerActivation, checkCfSecretConfig);
        Assertions.assertThrows(NullPointerException.class, checkCfWebClient::init);
    }

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {

        assertThrows(PnInternalException.class, () -> checkCfWebclient.buildSslContext(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        CheckCfWebClientConfig webClientConfig = new CheckCfWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(true, "", "", webClientConfig, ssmParameterConsumerActivation, checkCfSecretConfig);

        assertThrows(PnInternalException.class, checkCfWebclient::init, "Input stream not contain valid certificates.");
    }

}
