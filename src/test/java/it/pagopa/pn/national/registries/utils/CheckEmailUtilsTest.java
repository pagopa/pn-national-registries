package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.utils.CheckEmailUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;

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