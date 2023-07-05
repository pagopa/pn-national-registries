package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.commons.utils.MDCUtils;
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

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


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

        when(pnNationalRegistriesSecretService.getPdndSecretValue(any(),any())).thenReturn(pdndSecretValue);

        AgidJwtTrackingEvidence agidJwtTrackingEvidence = new AgidJwtTrackingEvidence(anprSecretConfig, kmsClient, pnNationalRegistriesSecretService);

        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");

        Assertions.assertThrows(NullPointerException.class, agidJwtTrackingEvidence::createAgidJwt);
    }

}
