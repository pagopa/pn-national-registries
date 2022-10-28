package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PnNationalRegistriesConfigTest {

    @Test
    void getAnprX509CertificateChain() {
        PnNationalRegistriesConfig pnNationalRegistriesConfig = new PnNationalRegistriesConfig();
        pnNationalRegistriesConfig.setAnprX509CertificateChain("test");
        Assertions.assertEquals("test", pnNationalRegistriesConfig.getAnprX509CertificateChain());
    }

    @Test
    void getAnprJWTHeaderDigestKeystoreAlias() {
        PnNationalRegistriesConfig pnNationalRegistriesConfig = new PnNationalRegistriesConfig();
        pnNationalRegistriesConfig.setAnprJWTHeaderDigestKeystoreAlias("test");
        Assertions.assertEquals("test", pnNationalRegistriesConfig.getAnprJWTHeaderDigestKeystoreAlias());
    }

    @Test
    void testToString(){
        PnNationalRegistriesConfig pnNationalRegistriesConfig = new PnNationalRegistriesConfig();
        pnNationalRegistriesConfig.setAnprJWTHeaderDigestKeystoreAlias("test");
        Assertions.assertEquals("test", pnNationalRegistriesConfig.getAnprJWTHeaderDigestKeystoreAlias());
        pnNationalRegistriesConfig.setAnprX509CertificateChain("test");
        Assertions.assertEquals("test", pnNationalRegistriesConfig.getAnprX509CertificateChain());
        Assertions.assertEquals("PnNationalRegistriesConfig(anprX509CertificateChain=test, anprJWTHeaderDigestKeystoreAlias=test)", pnNationalRegistriesConfig.toString());
    }
}
