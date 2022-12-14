package it.pagopa.pn.national.registries.config.checkcf;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
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
class CheckCfSecretConfigTest {

    @MockBean
    CheckCfSecretConfig checkCfSecretConfig;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getCheckCfSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse2 = GetSecretValueResponse.builder()
                .secretString("{\n" +
                        "\"keyId\":\"pub\",\n" +
                        "\"clientId\":\"trust\"\n" +
                        "}").build();
        when(secretManagerService.getSecretValue("test1"))
                .thenReturn(Optional.of(getSecretValueResponse2));
        CheckCfSecretConfig checkCfSecretConfig = new CheckCfSecretConfig(secretManagerService,"test1");
        Assertions.assertNotNull(checkCfSecretConfig.getCheckCfSecretValue());
    }

}
