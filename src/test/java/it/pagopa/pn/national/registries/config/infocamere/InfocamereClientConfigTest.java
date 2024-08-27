package it.pagopa.pn.national.registries.config.infocamere;

import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class InfocamereClientConfigTest {
    private static final String BASE_PATH = "basePath";

    /**
     * Method under test: {@link InfocamereClientConfig#authenticationApi(String)}
     */
    @Test
    void testAuthenticationApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig();

        AuthenticationApi authenticationApi = infocamereClientConfig.authenticationApi(BASE_PATH);
        assertEquals(BASE_PATH, authenticationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentationApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig();

        LegalRepresentationApi legalRepresentationApi = infocamereClientConfig.legalRepresentationApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentationApi.getApiClient().getBasePath());
    }

    @Test
    void testLegalRepresentativeApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig();

        LegalRepresentativeApi legalRepresentativeApi = infocamereClientConfig.legalRepresentativeApi(BASE_PATH);
        assertEquals(BASE_PATH, legalRepresentativeApi.getApiClient().getBasePath());
    }

    @Test
    void testPecApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig();

        PecApi pecApi = infocamereClientConfig.pecApi(BASE_PATH);
        assertEquals(BASE_PATH, pecApi.getApiClient().getBasePath());
    }

    @Test
    void testSedeApi() {
        InfocamereClientConfig infocamereClientConfig = new InfocamereClientConfig();

        SedeApi sedeApi = infocamereClientConfig.sedeApi(BASE_PATH);
        assertEquals(BASE_PATH, sedeApi.getApiClient().getBasePath());
    }
}
