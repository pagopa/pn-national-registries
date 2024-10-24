package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskTaxIdInPathUtilsTest {

    @Test
    void testMaskTaxIdInPathInad() {
        String message = "/extract/12345?";
        String expected = "/extract/***?";
        String actual = MaskTaxIdInPathUtils.maskTaxIdInPathInad(message);
        assertEquals(expected, actual);
    }

    @Test
    void testMaskTaxIdInPathICRegistroImprese() {
        String message = "/sede/12345?";
        String expected = "/sede/***?";
        String actual = MaskTaxIdInPathUtils.maskTaxIdInPathICRegistroImprese(message);
        assertEquals(expected, actual);
    }
}