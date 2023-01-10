package it.pagopa.pn.national.registries.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigitalAddressTypeTest {

    @Test
    void testFromValue() {
        assertEquals(DigitalAddressType.PEC, DigitalAddressType.fromValue("PEC"));
        assertThrows(IllegalArgumentException.class, () -> DigitalAddressType.fromValue("random"));
    }

}