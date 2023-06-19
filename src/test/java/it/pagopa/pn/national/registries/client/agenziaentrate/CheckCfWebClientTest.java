package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CheckCfWebClientTest {

    @InjectMocks
    CheckCfWebClient checkCfWebclient;
    
    @Mock
    CheckCfSecretConfig checkCfSecretConfig;

    @Mock
    SsmParameterConsumerActivation ssmParameterConsumerActivation;

    @Mock
    SecretManagerService secretManagerService;

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

        SSLData sslData = new SSLData();
        sslData.setSecretid("secretId");
        sslData.setCert("cert");

        GetSecretValueResponse secretsManagerResponse = GetSecretValueResponse.builder()
                        .secretString("secretString").build();

        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(true, "", "", webClientConfig, ssmParameterConsumerActivation, checkCfSecretConfig, secretManagerService);
        Mockito.when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.of(sslData));
        Mockito.when(secretManagerService.getSecretValue(any())).thenReturn(Optional.of(secretsManagerResponse));
        assertThrows(IllegalArgumentException.class, checkCfWebclient::init, "Input stream not contain valid certificates.");
    }

    @Test
    void initWhenSecretValueIsFoundThenReturnWebClient2() {
        CheckCfWebClientConfig webClientConfig = new CheckCfWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        SSLData sslData = new SSLData();
        sslData.setSecretid("secretId");
        sslData.setCert("cert");

        GetSecretValueResponse secretsManagerResponse = GetSecretValueResponse.builder()
                .secretString("secretString").build();

        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(true, "", "", webClientConfig, ssmParameterConsumerActivation, checkCfSecretConfig, secretManagerService);
        Mockito.when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.of(sslData));
        Mockito.when(secretManagerService.getSecretValue(any())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, checkCfWebclient::init, "Errore durante la chiamata al servizio VerificaCodiceFiscale");
    }

    @Test
    void initWhenSecretValueIsFoundThenReturnWebClient3() {
        CheckCfWebClientConfig webClientConfig = new CheckCfWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);

        SSLData sslData = new SSLData();
        sslData.setSecretid("secretId");
        sslData.setCert("cert");

        GetSecretValueResponse secretsManagerResponse = GetSecretValueResponse.builder()
                .secretString("secretString").build();

        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(true, "", "", webClientConfig, ssmParameterConsumerActivation, checkCfSecretConfig, secretManagerService);
        Mockito.when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, checkCfWebclient::init, "Errore durante la chiamata al servizio VerificaCodiceFiscale");
    }

}
