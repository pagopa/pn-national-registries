package it.pagopa.pn.national.registries.client;

import com.amazonaws.util.StringUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public abstract class CommonWebClient extends CommonBaseClient {

    @Autowired
    ResponseExchangeFilter responseExchangeFilter;

    protected Boolean sslCertVer;

    protected CommonWebClient(Boolean sslCertVer) {
        this.sslCertVer = sslCertVer;
    }

    protected final WebClient initWebClient(HttpClient httpClient, String baseUrl) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.registerDefaults(true);
                    configurer.customCodecs().register(new CustomFormMessageWriter());
                })
                .build();

        return super.initWebClient(WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .codecs(c -> c.defaultCodecs().enableLoggingRequestDetails(true))
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(responseExchangeFilter))
                .clientConnector(new ReactorClientHttpConnector(httpClient)));
    }

    protected final SslContext getSslContext(SslContextBuilder sslContextBuilder, String trust) throws SSLException {
        boolean notHasTrust = StringUtils.isNullOrEmpty(trust);
        if (notHasTrust && Boolean.FALSE.equals(sslCertVer)) {
            return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else if (notHasTrust) {
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
}
