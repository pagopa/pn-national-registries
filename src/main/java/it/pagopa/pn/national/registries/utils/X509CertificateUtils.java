package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
public class X509CertificateUtils {
    private final SSLData adeLegalSecret;

    @Getter
    private final X509Certificate certificate;

    public X509CertificateUtils(AdeLegalSecretConfig adeLegalSecretConfig) {
        this.adeLegalSecret = adeLegalSecretConfig.getAdeSecretConfig();
        this.certificate = loadCertificate();
    }

    public PrivateKey getPrivateKey() {
        byte[] array = Base64.getDecoder().decode(adeLegalSecret.getKey());

        String str = new String(array, StandardCharsets.UTF_8);
        str = str.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .trim();

        try {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(str));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(encodedKeySpec);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADE_LEGAL_LOAD_KEY, ERROR_CODE_ADE_LEGAL_LOAD_KEY, e);
        }
    }

    public X509Certificate loadCertificate() {
        byte[] array = Base64.getDecoder().decode(adeLegalSecret.getCert());

        String str = new String(array, StandardCharsets.UTF_8);
        str = str.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .trim();

        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(str));
        try {
            CertificateFactory kf = CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) kf.generateCertificate(is);
        } catch (CertificateException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADE_LEGAL_LOAD_CERT, ERROR_CODE_ADE_LEGAL_LOAD_CERT, e);
        }
    }

    public BigInteger getSerialNumber() {
        return certificate.getSerialNumber();
    }

    public String getIssuerName() {
        return certificate.getIssuerX500Principal().getName(X500Principal.RFC1779);
    }
}
