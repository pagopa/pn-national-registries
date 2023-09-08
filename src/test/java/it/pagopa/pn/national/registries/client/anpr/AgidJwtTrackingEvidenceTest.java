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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.kms.KmsClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void testCreateAgidJWT2() {
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


        Map<String, String> mdcMap = Map.of("trace_id", "Root=testtesttest;P");

        when(pnNationalRegistriesSecretService.getPdndSecretValue(any(),any())).thenReturn(pdndSecretValue);

        try (MockedStatic<MDCUtils> utilities = Mockito.mockStatic(MDCUtils.class)) {
            utilities.when(MDCUtils::retrieveMDCContextMap).thenReturn(mdcMap);


            AgidJwtTrackingEvidence agidJwtTrackingEvidence = new AgidJwtTrackingEvidence(anprSecretConfig, kmsClient, pnNationalRegistriesSecretService);

            SSLData sslData = new SSLData();
            sslData.setCert("TestCert");

            Assertions.assertThrows(NullPointerException.class, agidJwtTrackingEvidence::createAgidJwt);
        }
    }

    @Test
    void testCreateAgidJWT3() {
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


        Map<String, String> mdcMap = Map.of("trace_id", "traceId:testtesttest");

        when(pnNationalRegistriesSecretService.getPdndSecretValue(any(),any())).thenReturn(pdndSecretValue);

        try (MockedStatic<MDCUtils> utilities = Mockito.mockStatic(MDCUtils.class)) {
            utilities.when(MDCUtils::retrieveMDCContextMap).thenReturn(mdcMap);


            AgidJwtTrackingEvidence agidJwtTrackingEvidence = new AgidJwtTrackingEvidence(anprSecretConfig, kmsClient, pnNationalRegistriesSecretService);

            SSLData sslData = new SSLData();
            sslData.setCert("TestCert");

            Assertions.assertThrows(NullPointerException.class, agidJwtTrackingEvidence::createAgidJwt);
        }
    }
}


