package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {AgidJwtSignature.class})
@ExtendWith(SpringExtension.class)
class AgidJwtSignatureTest {

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @MockBean
    KmsClient kmsClient;

    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void testCreateAgidJWT() {
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("audience");
        jwtConfig.setKid("kid");
        jwtConfig.setIssuer("issuer");
        jwtConfig.setPurposeId("purposeId");
        jwtConfig.setSubject("subject");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("keyId");
        pdndSecretValue.setClientId("clientId");
        pdndSecretValue.setAuditDigest("audit");
        pdndSecretValue.setEserviceAudience("sservice");

        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(pdndSecretValue);

        AgidJwtSignature agidJwtSignature = new AgidJwtSignature(anprSecretConfig, kmsClient, pnNationalRegistriesSecretService);
        String digest = "digest";


        Assertions.assertThrows(NullPointerException.class, () -> agidJwtSignature.createAgidJwt(digest));
    }

}
