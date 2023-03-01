package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckCfWebClientTest {

    @InjectMocks
    CheckCfWebClient checkCfWebclient;
    
    @Mock
    CheckCfSecretConfig checkCfSecretConfig;

    @Test
    void testInit(){
        CheckCfWebClient checkCfWebClient = new CheckCfWebClient(100,100,
                100,100,"test.it",checkCfSecretConfig);
        Assertions.assertThrows(NullPointerException.class, checkCfWebClient::init);
    }




    @Test
    @DisplayName("Should return sslcontext with trustmanager when trust is not empty")
    void getSslContextWhenTrustIsNotEmptyThenReturnSslContextWithTrustManager(){
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        assertThrows(IllegalArgumentException.class, () -> checkCfWebclient.getSslContext(sslContextBuilder, sslData), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return an input stream when the clientkeypem is null")
    void getTrustCertInputStreamWhenClientKeyPemIsNullThenReturnInputStream() {
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        assertNotNull(checkCfWebclient.getTrustCertInputStream(sslData.getTrust()));
    }

    @Test
    @DisplayName("Should return an input stream when the clientkeypem is not null")
    void getTrustCertInputStreamWhenClientKeyPemIsNotNullThenReturnInputStream() {
        String clientKeyPem = "dGVzdA==";
        assertNotNull(checkCfWebclient.getTrustCertInputStream(clientKeyPem));
    }

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {

        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        when(checkCfSecretConfig.getCheckCfAuthChannelSecret()).thenReturn(sslData);

        assertThrows(IllegalArgumentException.class, () -> checkCfWebclient.buildSSLHttpClient(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        CheckCfWebClient checkCfWebclient = new CheckCfWebClient(100, 100, 100,
                100, "", checkCfSecretConfig);
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        sslData.setPub("pub");
        sslData.setTrust("trust");
        when(checkCfSecretConfig.getCheckCfAuthChannelSecret()).thenReturn(sslData);

        assertThrows(IllegalArgumentException.class, checkCfWebclient::init, "Input stream not contain valid certificates.");
    }

    @Test
    void getSslContextTest(){
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        sslData.setCert("");
        sslData.setKey("");
        sslData.setPub("");
        Assertions.assertThrows(Exception.class,()->checkCfWebclient.getSslContext(SslContextBuilder.forClient(),sslData));
    }

    @Test
    void getSslContextTest2() throws SSLException {
        SSLData sslData = new SSLData();
        sslData.setTrust(null);
        Assertions.assertNotNull(checkCfWebclient.getSslContext(SslContextBuilder.forClient(),sslData));
    }

}
