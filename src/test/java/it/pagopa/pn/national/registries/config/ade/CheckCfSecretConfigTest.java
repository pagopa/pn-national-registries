package it.pagopa.pn.national.registries.config.ade;

import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckCfSecretConfigTest {

    @Test
    void getCheckCfSecretConfigTest() {
        CheckCfSecretConfig checkCfSecretConfig = new CheckCfSecretConfig("test2", "trustedCert");
        Assertions.assertNotNull(checkCfSecretConfig.getPdndSecret());
        Assertions.assertNotNull(checkCfSecretConfig.getTrustData());
    }

}
