package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.infocamere.InfoCamereSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {InfoCamereJwsGenerator.class, String.class})
@ExtendWith(SpringExtension.class)
class InfoCamereJwsGeneratorTest {

    @Autowired
    private InfoCamereJwsGenerator authRest;
    @MockBean
    private InfoCamereSecretConfig infoCamereSecretConfig;

    @Test
    void testCreateAuthRest() {
        InfoCamereJwsGenerator authRest = new InfoCamereJwsGenerator("aud", "clientID", infoCamereSecretConfig);
        String scope = "test_scope";
        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");
        sslData.setKey("TestKey");
        sslData.setPub("TestPub");
        sslData.setTrust("TestTrust");
        Mockito.when(infoCamereSecretConfig.getInfoCamereAuthRestSecret()).thenReturn(sslData);
        Assertions.assertThrows(PnInternalException.class, () -> authRest.createAuthRest(scope));
    }


}
