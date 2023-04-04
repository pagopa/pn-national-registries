package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.spec.InvalidKeySpecException;

@ExtendWith(MockitoExtension.class)
class AgidJwtSignatureTest {

    @Mock
    AnprSecretConfig anprSecretConfig;

    @Test
    void testCreateAgidJWT() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("aud", anprSecretConfig);
        String digest = "digest";

        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId("test");
        pdndSecretValue.setKeyId("test");
        pdndSecretValue.setJwtConfig(new JwtConfig());
        Mockito.when(anprSecretConfig.getAnprPdndSecretValue()).thenReturn(pdndSecretValue);

        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");
        sslData.setKey("TestKey");
        sslData.setPub("TestPub");
        sslData.setTrust("TestTrust");
        Mockito.when(anprSecretConfig.getAnprIntegritySecret()).thenReturn(sslData);
        Assertions.assertThrows(PnInternalException.class,()->agidJwtSignature.createAgidJwt(digest));
    }
    @Test
    void testgetPrivateKey() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("secret1",anprSecretConfig);
        Assertions.assertThrows(InvalidKeySpecException.class,()->agidJwtSignature.getPrivateKey("dGVzdA=="));
    }

}

