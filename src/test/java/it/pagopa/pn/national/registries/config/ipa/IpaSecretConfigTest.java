package it.pagopa.pn.national.registries.config.ipa;

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
class IpaSecretConfigTest {

    @MockBean
    IpaSecretConfig ipaSecretConfig;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getIpaSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("""
                {
                "AUTH_ID":"AUTH_ID"
                }""").build();
        when(secretManagerService.getSecretValue("test1"))
                .thenReturn(Optional.of(getSecretValueResponse));
        IpaSecretConfig ipaSecretConfig = new IpaSecretConfig(secretManagerService, "test1");
        Assertions.assertNotNull(ipaSecretConfig.getIpaSecret());
    }
}
