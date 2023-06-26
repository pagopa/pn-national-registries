package it.pagopa.pn.national.registries.config.checkcf;

import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckCfSecretConfigTest {

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getCheckCfSecretConfigTest() {
        CheckCfSecretConfig checkCfSecretConfig = new CheckCfSecretConfig("test1","test2", "trustedCert");
        Assertions.assertNotNull(checkCfSecretConfig.getPdndSecret());
        Assertions.assertNotNull(checkCfSecretConfig.getPurposeId());
        Assertions.assertNotNull(checkCfSecretConfig.getTrustData());
    }

}
