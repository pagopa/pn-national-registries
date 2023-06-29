package it.pagopa.pn.national.registries.config.ade;

import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
@ExtendWith(MockitoExtension.class)
class AdeLegalSecretConfigTest {

    @Test
    void testConstructor() {
        AdeLegalSecretConfig actualAdeLegalSecretConfig = new AdeLegalSecretConfig("Auth Channel Data", new SsmParameterConsumerActivation(null), null, "Trust Data");
        assertNotNull(actualAdeLegalSecretConfig);
    }
}
