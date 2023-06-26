package it.pagopa.pn.national.registries.config.anpr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AnprSecretConfigTest {

    @Test
    void getAnprSecretConfigTest() {
        AnprSecretConfig anprSecretConfig = new AnprSecretConfig("trustSecret", "purposeId", "pdndSecretName", "env");
        Assertions.assertNotNull(anprSecretConfig.getTrustSecret());
    }
}
