package it.pagopa.pn.national.registries.client;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.net.ssl.SSLException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SecureWebClientUtilsTest {

    @Mock
    private SslContextBuilder sslContextBuilder;

    private final SecureWebClientUtils secureWebClientUtils = new SecureWebClientUtils();

    @Test
    void getSslContextWithTrust() throws SSLException {
        SslContext sslContext = mock(SslContext.class);
        when(sslContextBuilder.trustManager(any(InputStream.class))).thenReturn(sslContextBuilder);
        when(sslContextBuilder.build()).thenReturn(sslContext);

        SslContext result = secureWebClientUtils.getSslContext(sslContextBuilder, "trust123");

        assertEquals(sslContext, result);
        verify(sslContextBuilder).trustManager(any(InputStream.class));
        verify(sslContextBuilder).build();
    }

    @Test
    void getSslContextWithoutTrust() throws SSLException {
        SslContext sslContext = mock(SslContext.class);
        when(sslContextBuilder.build()).thenReturn(sslContext);

        SslContext result = secureWebClientUtils.getSslContext(sslContextBuilder, null);

        assertEquals(sslContext, result);
        verify(sslContextBuilder, never()).trustManager(any(InputStream.class));
        verify(sslContextBuilder).build();
    }

    @Test
    void getTrustCertInputStream() {
        String clientKeyPem = "clientKeyPem";
        InputStream result = secureWebClientUtils.getTrustCertInputStream(clientKeyPem);

        assertNotNull(result);
    }

    @Test
    void getKeyInputStream() {
        String clientKeyPem = "clientKeyPem";
        InputStream result = secureWebClientUtils.getKeyInputStream(clientKeyPem);

        assertNotNull(result);
    }

    @Test
    void getCertInputStream() {
        String stringCert = "stringCert";
        InputStream result = secureWebClientUtils.getCertInputStream(stringCert);

        assertNotNull(result);
    }
}