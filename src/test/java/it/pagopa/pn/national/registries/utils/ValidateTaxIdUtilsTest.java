package it.pagopa.pn.national.registries.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.junit.jupiter.api.Test;

class ValidateTaxIdUtilsTest {
    /**
     * Method under test: {@link ValidateTaxIdUtils#validateTaxId(String)}
     */
    @Test
    void testValidateTaxId() {
        assertThrows(PnNationalRegistriesException.class, () -> ValidateTaxIdUtils.validateTaxId("42"));
    }
}

