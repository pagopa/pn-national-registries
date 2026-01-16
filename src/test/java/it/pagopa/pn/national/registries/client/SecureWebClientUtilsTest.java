package it.pagopa.pn.national.registries.client;

import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.config.adecheckcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.utils.X509CertificateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {SecureWebClientUtils.class})
@ExtendWith(SpringExtension.class)
class SecureWebClientUtilsTest {
    @Autowired
    private SecureWebClientUtils secureWebClientUtils;
    @MockitoBean
    private CheckCfSecretConfig checkCfSecretConfig;

    @MockitoBean
    private X509CertificateUtils x509CertificateUtils;


    @Test
    void getSslContextForAde_shouldReturnSslContextWhenTrustIsProvided() throws SSLException {
        SslContextBuilder sslContextBuilder = mock(SslContextBuilder.class);
        when(sslContextBuilder.build()).thenReturn(mock(SslContext.class));
        when(sslContextBuilder.trustManager(any(InputStream.class))).thenReturn(sslContextBuilder);
        when(sslContextBuilder.keyManager(any(PrivateKey.class), any(X509Certificate.class))).thenReturn(sslContextBuilder);

        SSLData sslData = new SSLData();
        when(x509CertificateUtils.getKeyAndCertificate(any())).thenReturn(sslData);
        when(x509CertificateUtils.getPrivateKey(any())).thenReturn(mock(PrivateKey.class));
        when(x509CertificateUtils.loadCertificate(any())).thenReturn(mock(X509Certificate.class));

        SslContext sslContext = secureWebClientUtils.getSslContextForAde(sslContextBuilder, "dHJ1c3QK");
        assertNotNull(sslContext);
        verify(sslContextBuilder).trustManager(any(InputStream.class));
        verify(sslContextBuilder).keyManager(any(PrivateKey.class), any(X509Certificate.class));
    }


    @Test
    void getSslContextForAde_NoTrust() throws SSLException {
        SslContextBuilder sslContextBuilder = mock(SslContextBuilder.class);
        when(sslContextBuilder.build()).thenReturn(mock(SslContext.class));
        SslContext sslContext = secureWebClientUtils.getSslContextForAde(sslContextBuilder, null);
        Assertions.assertNotNull(sslContext);
    }

    /**
     * Method under test:
     * {@link SecureWebClientUtils#getSslContext(SslContextBuilder, String)}
     */
    @Test
    void testGetSslContext() throws SSLException {
        // Arrange and Act
        SslContext actualSslContext = secureWebClientUtils.getSslContext(SslContextBuilder.forClient(), "");

        // Assert
        assertNull(((JdkSslClientContext) actualSslContext).context()
                .getDefaultSSLParameters()
                .getEndpointIdentificationAlgorithm());
        assertTrue(actualSslContext.isClient());
    }

    /**
     * Method under test:
     * {@link SecureWebClientUtils#getSslContext(SslContextBuilder, String)}
     */
    @Test
    void testGetSslContext2() throws SSLException {
        // Arrange
        SslContextBuilder sslContextBuilder = mock(SslContextBuilder.class);
        when(sslContextBuilder.build()).thenReturn(mock(SslContext.class));

        when(sslContextBuilder.trustManager((InputStream) any())).thenReturn(sslContextBuilder);
        when(sslContextBuilder.keyManager(any(PrivateKey.class), any(X509Certificate.class))).thenReturn(sslContextBuilder);
        when(x509CertificateUtils.getKeyAndCertificate(any())).thenReturn(new SSLData());
        when(x509CertificateUtils.getPrivateKey(any())).thenReturn(mock(PrivateKey.class));
        when(x509CertificateUtils.loadCertificate(any())).thenReturn(mock(X509Certificate.class));

        // Act
        SslContext actualSslContext = secureWebClientUtils.getSslContext(sslContextBuilder, "dHJ1c3QK");

        // Assert
        assertNotNull(actualSslContext);
        verify(sslContextBuilder).trustManager((InputStream) any());
    }
}
