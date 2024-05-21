package it.pagopa.pn.national.registries.client;

import com.amazonaws.util.StringUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class SecureWebClientUtils {

    public final SslContext getSslContext(SslContextBuilder sslContextBuilder, String trust) throws SSLException {
        boolean notHasTrust = StringUtils.isNullOrEmpty(trust);
        if (notHasTrust) {
            return sslContextBuilder.build();
        }
        return sslContextBuilder.trustManager(getTrustCertInputStream(trust)).build();
    }

    public final InputStream getTrustCertInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }

    public final InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(clientKeyPem.getBytes(StandardCharsets.UTF_8));
    }

    public final InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(stringCert));
    }

}
