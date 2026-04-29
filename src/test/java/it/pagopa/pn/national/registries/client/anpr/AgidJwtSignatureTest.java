package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgidJwtSignatureTest {

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    KmsClient kmsClient;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @InjectMocks
    AgidJwtSignature agidJwtSignature;

    @BeforeEach
    void setup(){
        NationalRegistriesConfig.Anpr anprConfig = new NationalRegistriesConfig.Anpr();
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anprConfig);
    }


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

        String digest = "digest";

        Assertions.assertThrows(NullPointerException.class, () -> agidJwtSignature.createAgidJwt(digest));
    }

}
