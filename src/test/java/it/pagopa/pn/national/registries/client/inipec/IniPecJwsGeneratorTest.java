package it.pagopa.pn.national.registries.client.inipec;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.spec.InvalidKeySpecException;

@ContextConfiguration(classes = {IniPecJwsGenerator.class, String.class})
@ExtendWith(SpringExtension.class)
class IniPecJwsGeneratorTest {

    @Autowired
    private IniPecJwsGenerator authRest;
    @MockBean
    private IniPecSecretConfig iniPecSecretConfig;

    @Test
    void testCreateAuthRest() {
        IniPecJwsGenerator authRest = new IniPecJwsGenerator("aud", "clientID", iniPecSecretConfig);

        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");
        sslData.setKey("TestKey");
        sslData.setPub("TestPub");
        sslData.setTrust("TestTrust");
        Mockito.when(iniPecSecretConfig.getIniPecAuthRestSecret()).thenReturn(sslData);
        Assertions.assertThrows(PnInternalException.class, authRest::createAuthRest);
    }

    @Test
    void testgetPrivateKey() {
        IniPecJwsGenerator authRest = new IniPecJwsGenerator("secret1","",iniPecSecretConfig);
        Assertions.assertThrows(InvalidKeySpecException.class,()->authRest.getPrivateKey("dGVzdA=="));
    }
}
