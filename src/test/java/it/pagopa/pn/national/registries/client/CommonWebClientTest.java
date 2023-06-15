package it.pagopa.pn.national.registries.client;

import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.*;

class CommonWebClientTest {

    static class TestCommonWebClient extends CommonWebClient {

        public TestCommonWebClient(Boolean sslCertVer) {
            super(sslCertVer);
        }

    }

    @Test
    void testGetSslContext1() throws SSLException {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        assertNotNull(webClient.getSslContext(SslContextBuilder.forClient(),""));
    }

    @Test
    void testGetSslContext2() throws SSLException {
        TestCommonWebClient webClient = new TestCommonWebClient(false);
        assertNotNull(webClient.getSslContext(SslContextBuilder.forClient(),""));
    }

    @Test
    void testGetSslContext3() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        Assertions.assertDoesNotThrow(() -> webClient.getSslContext(sslContextBuilder,""));
    }

    @Test
    void testGetTrustCertInputStream() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        assertNotNull(webClient.getTrustCertInputStream("dGVzdA=="));
    }
}