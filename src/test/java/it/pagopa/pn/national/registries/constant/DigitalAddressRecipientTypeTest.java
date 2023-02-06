package it.pagopa.pn.national.registries.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigitalAddressRecipientTypeTest {

    @Test
    void testFromValue() {
        assertEquals(DigitalAddressRecipientType.IMPRESA, DigitalAddressRecipientType.fromValue("IMPRESA"));
        assertThrows(IllegalArgumentException.class, () -> DigitalAddressRecipientType.fromValue("random"));
    }
    @Test
    void testToString() {
        assertEquals("IMPRESA", DigitalAddressRecipientType.IMPRESA.toString());
    }


}