package it.pagopa.pn.national.registries.config.ipa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IpaSecretConfigTest {

    @Test
    void getIpaSecretConfigTest() {
        IpaSecretConfig ipaSecretConfig = new IpaSecretConfig("test1");
        Assertions.assertNotNull(ipaSecretConfig.getIpaSecret());
    }
}
