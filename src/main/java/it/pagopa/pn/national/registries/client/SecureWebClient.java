package it.pagopa.pn.national.registries.client;

import com.amazonaws.util.StringUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public abstract class SecureWebClient extends CommonBaseClient {

    @Autowired
    ResponseExchangeFilter responseExchangeFilter;

    protected SecureWebClient() {
    }

    protected final WebClient initWebClient(String baseUrl) {
        return super.enrichBuilder(WebClient.builder().baseUrl(baseUrl))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .build();
    }

    protected final SslContext getSslContext(SslContextBuilder sslContextBuilder, String trust) throws SSLException {
        boolean notHasTrust = StringUtils.isNullOrEmpty(trust);
        if (notHasTrust) {
            return sslContextBuilder.build();
        }
        return sslContextBuilder.trustManager(getTrustCertInputStream(trust)).build();
    }

    protected final InputStream getTrustCertInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }

    protected final InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(clientKeyPem.getBytes(StandardCharsets.UTF_8));
    }

    protected final InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(stringCert));
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient()
                .secure(t -> t.sslContext(buildSslContext()));
    }

    protected abstract SslContext buildSslContext();
}
