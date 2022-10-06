package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PnExternalRegistriesConfigTest {

    @Test
    void getAnprX509CertificateChain() {
        PnExternalRegistriesConfig pnExternalRegistriesConfig = new PnExternalRegistriesConfig();
        pnExternalRegistriesConfig.setAnprX509CertificateChain("test");
        Assertions.assertEquals("test",pnExternalRegistriesConfig.getAnprX509CertificateChain());
    }

    @Test
    void getAnprJWTHeaderDigestKeystoreAlias() {
        PnExternalRegistriesConfig pnExternalRegistriesConfig = new PnExternalRegistriesConfig();
        pnExternalRegistriesConfig.setAnprJWTHeaderDigestKeystoreAlias("test");
        Assertions.assertEquals("test",pnExternalRegistriesConfig.getAnprJWTHeaderDigestKeystoreAlias());
    }

    @Test
    void testToString(){
        PnExternalRegistriesConfig pnExternalRegistriesConfig = new PnExternalRegistriesConfig();
        pnExternalRegistriesConfig.setAnprJWTHeaderDigestKeystoreAlias("test");
        Assertions.assertEquals("test",pnExternalRegistriesConfig.getAnprJWTHeaderDigestKeystoreAlias());
        pnExternalRegistriesConfig.setAnprX509CertificateChain("test");
        Assertions.assertEquals("test",pnExternalRegistriesConfig.getAnprX509CertificateChain());
        Assertions.assertEquals("PnExternalRegistriesConfig(anprX509CertificateChain=test, anprJWTHeaderDigestKeystoreAlias=test)",pnExternalRegistriesConfig.toString());
    }
}
