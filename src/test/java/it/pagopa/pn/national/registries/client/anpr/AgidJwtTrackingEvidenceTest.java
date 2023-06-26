package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;


@ExtendWith(SpringExtension.class)
class AgidJwtTrackingEvidenceTest {

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @MockBean
    KmsClient kmsClient;

    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void testCreateAgidJWT() {
        AgidJwtTrackingEvidence agidJwtTrackingEvidence = new AgidJwtTrackingEvidence(anprSecretConfig, kmsClient, pnNationalRegistriesSecretService);

        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId("test");
        pdndSecretValue.setKeyId("test");
        pdndSecretValue.setJwtConfig(new JwtConfig());

        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");

        Assertions.assertThrows(NullPointerException.class, agidJwtTrackingEvidence::createAgidJwt);
    }

}

