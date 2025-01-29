package it.pagopa.pn.national.registries.client;

import com.amazonaws.util.StringUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.national.registries.config.adecheckcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.utils.X509CertificateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureWebClientUtils {

    private final CheckCfSecretConfig checkCfSecretConfig;
    private final X509CertificateUtils x509CertificateUtils;

    public final SslContext getSslContext(SslContextBuilder sslContextBuilder, String trust) throws SSLException {
        boolean notHasTrust = StringUtils.isNullOrEmpty(trust);
        if (notHasTrust) {
            return sslContextBuilder.build();
        }
        SSLData sslData = x509CertificateUtils.getKeyAndCertificate(checkCfSecretConfig.getAuthChannelData());
        return sslContextBuilder.trustManager(getTrustCertInputStream(trust))
                .keyManager(x509CertificateUtils.getPrivateKey(sslData.getSecretid()), x509CertificateUtils.loadCertificate(sslData.getCert())).build();
    }



    protected final InputStream getTrustCertInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }

}
