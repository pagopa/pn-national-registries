package it.pagopa.pn.national.registries.config.adecheckcf;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckCfSecretConfigTest {

    @Test
    void getCheckCfSecretConfigTest() {
        CheckCfSecretConfig checkCfSecretConfig = new CheckCfSecretConfig("test2", "trustedCert", "");
        Assertions.assertNotNull(checkCfSecretConfig.getPdndSecret());
        Assertions.assertNotNull(checkCfSecretConfig.getTrustData());
    }

}
