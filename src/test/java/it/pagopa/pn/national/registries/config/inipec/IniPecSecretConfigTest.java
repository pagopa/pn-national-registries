package it.pagopa.pn.national.registries.config.inipec;

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
class IniPecSecretConfigTest {

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void getIniPecSecretConfigTest() {
        GetSecretValueResponse getSecretValueResponse2 = GetSecretValueResponse.builder()
                .secretString("{\n" +
                        "\"cert\":\"cert\",\n" +
                        "\"key\":\"key\"\n" +
                        "}").build();
        when(secretManagerService.getSecretValue("test1"))
                .thenReturn(Optional.of(getSecretValueResponse2));
        IniPecSecretConfig inipecSecretConfig = new IniPecSecretConfig(secretManagerService,"test1");
        Assertions.assertNotNull(inipecSecretConfig.getIniPecAuthRestSecret());
    }

}
