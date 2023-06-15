package it.pagopa.pn.national.registries.config.checkcf;

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
class CheckCfSecretConfigTest {

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getCheckCfSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse2 = GetSecretValueResponse.builder()
                .secretString("""
                        {
                        "keyId":"trust"
                        }""").build();
        GetSecretValueResponse getSecretValueResponse1 = GetSecretValueResponse.builder()
                .secretString("""
                        {
                        "trust":"trust"
                        }""").build();
        when(secretManagerService.getSecretValue("test2"))
                .thenReturn(Optional.of(getSecretValueResponse2));
        when(secretManagerService.getSecretValue("trustedCert"))
                .thenReturn(Optional.of(getSecretValueResponse1));

        CheckCfSecretConfig checkCfSecretConfig = new CheckCfSecretConfig(secretManagerService,"test1","test2", "trustedCert");
        Assertions.assertNotNull(checkCfSecretConfig.getCheckCfPdndSecretValue());
    }

}
