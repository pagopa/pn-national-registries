package it.pagopa.pn.national.registries.client.inipec;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.anpr.AgidJwtSignature;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SSLData;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import it.pagopa.pn.national.registries.model.SecretValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {AgidJwtSign.class, String.class})
@ExtendWith(SpringExtension.class)
class AgidJwtSignTest {
    @Autowired
    private AgidJwtSign agidJwtSign;

    @MockBean
    private IniPecSecretConfig iniPecSecretConfig;

    @Test
    void testCreateAgidJWT() {
        AgidJwtSign agidJwtSign = new AgidJwtSign("aud", iniPecSecretConfig);
        String digest = "digest";

        SecretValue secretValue = new SecretValue();
        secretValue.setClientId("test");
        secretValue.setKeyId("test");
        secretValue.setJwtConfig(new JwtConfig());
        Mockito.when(iniPecSecretConfig.getIniPecSecretValue()).thenReturn(secretValue);

        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");
        sslData.setKey("TestKey");
        sslData.setPub("TestPub");
        sslData.setTrust("TestTrust");
        Mockito.when(iniPecSecretConfig.getIniPecIntegritySecret()).thenReturn(sslData);
        Assertions.assertThrows(PnInternalException.class,()->agidJwtSign.createAgidJwt(digest));
    }

    @Test
    void testgetPrivateKey() {
        AgidJwtSign agidJwtSign = new AgidJwtSign("secret1",iniPecSecretConfig);
        Assertions.assertThrows(InvalidKeySpecException.class,()->agidJwtSign.getPrivateKey("dGVzdA=="));
    }
}

