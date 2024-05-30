package it.pagopa.pn.national.registries.client;

import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.net.ssl.SSLException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {SecureWebClientUtils.class})
@ExtendWith(SpringExtension.class)
class SecureWebClientUtilsTest {
    @Autowired
    private SecureWebClientUtils secureWebClientUtils;

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

        // Act
        SslContext actualSslContext = secureWebClientUtils.getSslContext(sslContextBuilder, "dHJ1c3QK");

        // Assert
        assertNotNull(actualSslContext);
        verify(sslContextBuilder).trustManager((InputStream) any());
    }
}
