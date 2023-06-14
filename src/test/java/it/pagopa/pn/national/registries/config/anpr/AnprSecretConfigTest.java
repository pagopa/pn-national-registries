package it.pagopa.pn.national.registries.config.anpr;

import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprSecretConfigTest {

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getAnprSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse2 = GetSecretValueResponse.builder().secretString("""
                {
                "keyId":"pub",
                "clientId":"trust"
                }""").build();
        when(secretManagerService.getSecretValue("test1"))
                .thenReturn(Optional.of(getSecretValueResponse2));
        AnprSecretConfig anprSecretConfig = new AnprSecretConfig(secretManagerService, "test1", "test1");
        Assertions.assertNotNull(anprSecretConfig.getAnprPdndSecretValue());
    }
}
