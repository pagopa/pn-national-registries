package it.pagopa.pn.national.registries.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MaskDataUtilsTest {
    @Test
    void testMaskInformation() {
        assertNull(MaskDataUtils.maskInformation(null));
        assertEquals("",MaskDataUtils.maskString(""));
        assertEquals("Data Buffered", MaskDataUtils.maskInformation("Data Buffered"));
        assertEquals("\"taxId\" : \"U*\"", MaskDataUtils.maskInformation("\"taxId\" : \"UU\""));
        assertEquals("\"taxId\" : \"*,*\"", MaskDataUtils.maskInformation("\"taxId\" : \"U,U\""));
        assertEquals("\"taxId\" : \"*@U\"", MaskDataUtils.maskInformation("\"taxId\" : \"U@U\""));
    }
}

