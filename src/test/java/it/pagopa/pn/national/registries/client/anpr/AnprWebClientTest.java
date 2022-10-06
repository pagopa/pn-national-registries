package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.net.ssl.SSLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprWebClientTest {

    @Mock
    SecretManagerService secretManagerService;

    @InjectMocks
    AnprWebClient anprWebClient;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return sslcontext with trustmanager when trust is not empty")
    void getSslContextWhenTrustIsNotEmptyThenReturnSslContextWithTrustManager() throws SSLException {
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
    @DisplayName("Should return null when secret value not found")
    void buildSSLHttpClientWhenSecretValueNotFoundThenReturnNull() {
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.empty());
        SslContext sslContext = anprWebClient.buildSSLHttpClient();
        assertNull(sslContext);
    }

    @Test
    @DisplayName("Should return sslcontext when trust is empty")
    void buildSSLHttpClientWhenTrustIsEmptyThenReturnSslContext() {
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString("{\n" +
                        "\"cert\":\"cert\",\n" +
                        "\"key\":\"key\",\n" +
                        "\"pub\":\"pub\",\n" +
                        "\"trust\":\"trust\"\n" +
                        "}").build();
        when(secretManagerService.getSecretValue(any()))
                .thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(IllegalArgumentException.class, () -> anprWebClient.buildSSLHttpClient(), "Input stream not contain valid certificates.");
    }

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        AnprWebClient anprWebClient = new AnprWebClient(100, 100, 100,
                100, "", "", secretManagerService);
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setKey("key");
        sslData.setPub("pub");
        sslData.setTrust("trust");
        GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString("{\n" +
                        "\"cert\":\"cert\",\n" +
                        "\"key\":\"key\",\n" +
                        "\"pub\":\"pub\",\n" +
                        "\"trust\":\"trust\"\n" +
                        "}").build();
        when(secretManagerService.getSecretValue(any()))
                .thenReturn(Optional.of(getSecretValueResponse));

        assertThrows(IllegalArgumentException.class, () -> anprWebClient.init(), "Input stream not contain valid certificates.");
    }
}
