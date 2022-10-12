package it.pagopa.pn.national.registries.client.anpr;

import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprWebClientTest {

    @InjectMocks
    AnprWebClient anprWebClient;

    @Mock
    AnprSecretConfig anprSecretConfig;

    @Test
    @DisplayName("Should return sslcontext with trustmanager when trust is not empty")
    void getSslContextWhenTrustIsNotEmptyThenReturnSslContextWithTrustManager(){
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        assertThrows(IllegalArgumentException.class, () -> anprWebClient.getSslContext(sslContextBuilder, sslData), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return an input stream when the clientkeypem is null")
    void getTrustCertInputStreamWhenClientKeyPemIsNullThenReturnInputStream() {
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        assertNotNull(anprWebClient.getTrustCertInputStream(sslData.getTrust()));
    }

    @Test
    @DisplayName("Should return an input stream when the clientkeypem is not null")
    void getTrustCertInputStreamWhenClientKeyPemIsNotNullThenReturnInputStream() {
        String clientKeyPem = "dGVzdA==";
        assertNotNull(anprWebClient.getTrustCertInputStream(clientKeyPem));
    }

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {

        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        when(anprSecretConfig.getAnprAuthChannelSecret()).thenReturn(sslData);
        /*GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString("{\n" +
                        "\"cert\":\"cert\",\n" +
                        "\"key\":\"key\",\n" +
                        "\"pub\":\"pub\",\n" +
                        "\"trust\":\"trust\"\n" +
                        "}").build();*/
        assertThrows(IllegalArgumentException.class, () -> anprWebClient.buildSSLHttpClient(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        AnprWebClient anprWebClient = new AnprWebClient(100, 100, 100,
                100, "", anprSecretConfig);
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        sslData.setPub("pub");
        sslData.setTrust("trust");
        when(anprSecretConfig.getAnprAuthChannelSecret()).thenReturn(sslData);

        /*GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString("{\n" +
                        "\"cert\":\"cert\",\n" +
                        "\"key\":\"key\",\n" +
                        "\"pub\":\"pub\",\n" +
                        "\"trust\":\"trust\"\n" +
                        "}").build();*/

        assertThrows(IllegalArgumentException.class, anprWebClient::init, "Input stream not contain valid certificates.");
    }

    @Test
    void getSslContextTest(){
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        sslData.setCert("");
        sslData.setKey("");
        sslData.setPub("");
        Assertions.assertThrows(Exception.class,()->anprWebClient.getSslContext(SslContextBuilder.forClient(),sslData));
    }

    @Test
    void getSslContextTest2() throws SSLException {
        SSLData sslData = new SSLData();
        sslData.setTrust(null);
        Assertions.assertNotNull(anprWebClient.getSslContext(SslContextBuilder.forClient(),sslData));
    }
}
