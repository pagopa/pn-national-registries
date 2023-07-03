package it.pagopa.pn.national.registries.config.inad;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InadSecretConfigTest {

    @Test
    void getInadSecretConfigTest() {
        InadSecretConfig inadSecretConfig = new InadSecretConfig("test1", "test2");
        Assertions.assertNotNull(inadSecretConfig.getPdndSecret());
        Assertions.assertNotNull(inadSecretConfig.getPurposeId());
    }

}
