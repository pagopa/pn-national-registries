package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidateTaxIdUtilsTest {
    private ValidateTaxIdUtils validateTaxIdUtils;

    private ValidateUtils validateUtils;

    /**
     * Method under test: {@link ValidateTaxIdUtils#validateTaxId(String)}
     */
    @BeforeEach
    void setup() {
        validateUtils = Mockito.mock( ValidateUtils.class );
        validateTaxIdUtils = new ValidateTaxIdUtils( validateUtils ); }

    @Test
    void testValidateTaxId() {
        assertThrows(PnNationalRegistriesException.class, () -> validateTaxIdUtils.validateTaxId("42"));
    }
}

