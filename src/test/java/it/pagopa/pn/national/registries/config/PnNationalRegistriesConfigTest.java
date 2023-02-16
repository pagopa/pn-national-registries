package it.pagopa.pn.national.registries.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PnNationalRegistriesConfigTest {

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link PnNationalRegistriesConfig}
     *   <li>{@link PnNationalRegistriesConfig#toString()}
     *   <li>{@link PnNationalRegistriesConfig#getAnprJWTHeaderDigestKeystoreAlias()}
     *   <li>{@link PnNationalRegistriesConfig#getAnprX509CertificateChain()}
     * </ul>
     */
    @Test
    void testConstructor() {
        PnNationalRegistriesConfig actualPnNationalRegistriesConfig = new PnNationalRegistriesConfig();
        String actualToStringResult = actualPnNationalRegistriesConfig.toString();
        assertNull(actualPnNationalRegistriesConfig.getAnprJWTHeaderDigestKeystoreAlias());
        assertNull(actualPnNationalRegistriesConfig.getAnprX509CertificateChain());
        assertEquals("PnNationalRegistriesConfig(anprX509CertificateChain=null, anprJWTHeaderDigestKeystoreAlias=null)",
                actualToStringResult);
    }
}
