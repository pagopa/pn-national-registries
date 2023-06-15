package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AgidJwtSignatureTest {

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @MockBean
    KmsClient kmsClient;

    @Test
    void testCreateAgidJWT() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature(anprSecretConfig, kmsClient);
        String digest = "digest";

        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId("test");
        pdndSecretValue.setKeyId("test");
        pdndSecretValue.setJwtConfig(new JwtConfig());
        when(anprSecretConfig.getAnprPdndSecretValue()).thenReturn(pdndSecretValue);

        Assertions.assertThrows(NullPointerException.class,()->agidJwtSignature.createAgidJwt(digest));
    }

}

