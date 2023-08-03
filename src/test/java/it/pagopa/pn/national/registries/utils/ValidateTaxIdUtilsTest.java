package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ValidateTaxIdUtilsTest {
    private ValidateTaxIdUtils validateTaxIdUtils;

    private ValidateUtils validateUtils;

    /**
     * Method under test: {@link ValidateTaxIdUtils#validateTaxId(String, String, boolean)}
     */
    @BeforeEach
    void setup() {
        validateUtils = Mockito.mock( ValidateUtils.class );
        validateTaxIdUtils = new ValidateTaxIdUtils( validateUtils ); }

    @Test
    void testValidateTaxId() {
        assertThrows(PnNationalRegistriesException.class, () -> validateTaxIdUtils.validateTaxId("42","", false));
    }

    @Test
    void testValidateTaxId2(){
        when(validateUtils.validate("42", false)).thenReturn(true);
        assertDoesNotThrow(() -> validateTaxIdUtils.validateTaxId("42","42",false));
    }
}

