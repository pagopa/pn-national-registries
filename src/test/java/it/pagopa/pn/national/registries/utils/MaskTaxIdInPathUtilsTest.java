package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskTaxIdInPathUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "/extract/12345?,/extract/***?",
            "/sede/12345?,/sede/***?",
            "/listaLegaleRappresentante/TESTCF?,/listaLegaleRappresentante/***?"
    })
    void testMaskTaxIdInPath(String message, String expected) {
        String actual = MaskTaxIdInPathUtils.maskTaxIdInPath(message);
        assertEquals(expected, actual);
    }
}