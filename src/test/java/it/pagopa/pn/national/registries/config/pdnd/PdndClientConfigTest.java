package it.pagopa.pn.national.registries.config.pdnd;

import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
class PdndClientConfigTest {
    private PdndClientConfig pdndClientConfig;

    @BeforeEach
    void setUp() {
        pdndClientConfig = new PdndClientConfig();
    }

    @Test
    void authApi() {
        AuthApi authApi = pdndClientConfig.authApi("basePath");
        assertNotNull(authApi);
        assertEquals("basePath", authApi.getApiClient().getBasePath());
    }
}