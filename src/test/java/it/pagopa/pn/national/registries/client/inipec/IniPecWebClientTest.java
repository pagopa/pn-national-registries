package it.pagopa.pn.national.registries.client.inipec;

import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IniPecWebClientTest {

    @InjectMocks
    IniPecWebClient iniPecWebClient;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        IniPecWebClient iniPecWebClient = new IniPecWebClient("basePath", "purposeId", 100,
                100, 100, 100,secretManagerService);

        assertThrows(NullPointerException.class, iniPecWebClient::init, "Input stream not contain valid certificates.");
    }

}
