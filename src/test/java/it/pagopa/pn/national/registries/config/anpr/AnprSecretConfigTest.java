package it.pagopa.pn.national.registries.config.anpr;

import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AnprSecretConfigTest {
    @MockBean
    AnprSecretConfig anprSecretConfig;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getAnprSecretConfigTest() {
        AnprSecretConfig anprSecretConfig1 = new AnprSecretConfig(secretManagerService,"test","test","test");
        assertNotNull(anprSecretConfig1);
    }
}