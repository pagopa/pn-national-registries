package it.pagopa.pn.national.registries.config.ade;

import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AdeLegalSecretConfigTest {

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getCheckCfSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder()
                .secretString("""
                        {
                        "cert":"cert",
                        "key":"key",
                        "pub":"pub",
                        "trust":"trust"
                        }""").build();
        when(secretManagerService.getSecretValue("test"))
                .thenReturn(Optional.of(getSecretValueResponse));

        AdeLegalSecretConfig adeLegalSecretConfig = new AdeLegalSecretConfig(secretManagerService,"test");
        Assertions.assertNotNull(adeLegalSecretConfig.getAdeSecretConfig());
    }
}
