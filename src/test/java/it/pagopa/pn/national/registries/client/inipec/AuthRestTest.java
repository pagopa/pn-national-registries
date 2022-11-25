package it.pagopa.pn.national.registries.client.inipec;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.SecretValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.spec.InvalidKeySpecException;

@ContextConfiguration(classes = {AuthRest.class, String.class})
@ExtendWith(SpringExtension.class)
class AuthRestTest {
    @Autowired
    private AuthRest authRest;
    @MockBean
    private IniPecSecretConfig iniPecSecretConfig;

    @Test
    void testCreateAuthRest() {
        AuthRest authRest = new AuthRest("aud", iniPecSecretConfig);

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
        Mockito.when(iniPecSecretConfig.getIniPecAuthRestSecret()).thenReturn(sslData);
        Assertions.assertThrows(PnInternalException.class,()->authRest.createAuthRest());
    }

    @Test
    void testgetPrivateKey() {
        AuthRest authRest = new AuthRest("secret1",iniPecSecretConfig);
        Assertions.assertThrows(InvalidKeySpecException.class,()->authRest.getPrivateKey("dGVzdA=="));
    }
}

