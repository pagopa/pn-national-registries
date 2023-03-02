package it.pagopa.pn.national.registries.client;

import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.model.SSLData;
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
        SSLData sslData = new SSLData();
        assertNotNull(webClient.getSslContext(SslContextBuilder.forClient(), sslData));
    }

    @Test
    void testGetSslContext2() throws SSLException {
        TestCommonWebClient webClient = new TestCommonWebClient(false);
        SSLData sslData = new SSLData();
        assertNotNull(webClient.getSslContext(SslContextBuilder.forClient(), sslData));
    }

    @Test
    void testGetSslContext3() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        SSLData sslData = new SSLData();
        sslData.setTrust("dGVzdA==");
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        assertThrows(IllegalArgumentException.class, () -> webClient.getSslContext(sslContextBuilder, sslData));
    }

    @Test
    void testGetTrustCertInputStream() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        assertNotNull(webClient.getTrustCertInputStream("dGVzdA=="));
    }

    @Test
    void testGetKeyInputStream() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        assertNotNull(webClient.getKeyInputStream("dGVzdA=="));
    }

    @Test
    void testGetCertInputStream() {
        TestCommonWebClient webClient = new TestCommonWebClient(true);
        assertNotNull(webClient.getCertInputStream("dGVzdA=="));
    }

}