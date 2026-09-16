package it.pagopa.pn.national.registries.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DigitalAddressRecipientTypeTest {

    @Test
    void testFromValue() {
        assertEquals(DigitalAddressRecipientType.IMPRESA, DigitalAddressRecipientType.fromValue("IMPRESA"));
        assertNull(DigitalAddressRecipientType.fromValue("random"));
    }
    @Test
    void testToString() {
        assertEquals("IMPRESA", DigitalAddressRecipientType.IMPRESA.toString());
    }


}