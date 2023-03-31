package it.pagopa.pn.national.registries.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import org.junit.jupiter.api.Test;

class CheckExceptionUtilsTest {

    /**
     * Method under test: {@link CheckExceptionUtils#isForLogLevelWarn(Throwable)}
     */
    @Test
    void testIsForLogLevelWarn() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("", 400, "", null,  null, null, null);
        assertTrue(CheckExceptionUtils.isForLogLevelWarn(exception));
    }

    /**
     * Method under test: {@link CheckExceptionUtils#isForLogLevelWarn(Throwable)}
     */
    @Test
    void testIsForLogLevelWarn1() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("", 500, "", null,  null, null, null);
        assertFalse(CheckExceptionUtils.isForLogLevelWarn(exception));
    }
}
