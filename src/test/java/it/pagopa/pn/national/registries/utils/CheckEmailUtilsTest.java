package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CheckEmailUtilsTest {

    @Test
    public void testIsValidEmailWithValidEmail() {
        Assertions.assertTrue(CheckEmailUtils.isValidEmail("test@example.com"));
    }

    @Test
    public void testIsValidEmailWithInvalidEmail() {
        Assertions.assertFalse(CheckEmailUtils.isValidEmail("invalid-email"));
    }

    @Test
    public void testIsValidEmailWithNull() {
        Assertions.assertFalse(CheckEmailUtils.isValidEmail(null));
    }
}