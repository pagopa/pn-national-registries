package it.pagopa.pn.national.registries.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsAnprAddressStrategyTest {

    private final UtilsAnprAddressStrategy utils = new UtilsAnprAddressStrategy() {
    };

    @Test
    void appendIfFitsNullValueDoesNothing() {
        StringBuilder sb = new StringBuilder();
        utils.appendIfFits(sb, null);
        assertEquals("", sb.toString());
    }

    @Test
    void appendIfFitsEmptyAndBlankValueDoNothing() {
        StringBuilder sb = new StringBuilder("abc");
        utils.appendIfFits(sb, "");
        utils.appendIfFits(sb, "   ");
        assertEquals("abc", sb.toString());
    }

    @Test
    void appendIfFitsFirstTokenIsStrippedAndHasNoLeadingSpace() {
        StringBuilder sb = new StringBuilder();
        utils.appendIfFits(sb, "  VIA  ");
        assertEquals("VIA", sb.toString());
    }

    @Test
    void appendIfFitsSecondTokenIsSeparatedBySpace() {
        StringBuilder sb = new StringBuilder();
        utils.appendIfFits(sb, "VIA");
        utils.appendIfFits(sb, "ROMA");
        assertEquals("VIA ROMA", sb.toString());
    }

    @Test
    void appendIfFitsTokenOfMaxLengthFitsOnEmptyBuilder() {
        StringBuilder sb = new StringBuilder();
        utils.appendIfFits(sb, "a".repeat(44));
        assertEquals(44, sb.length());
    }

    @Test
    void appendIfFitsTokenLongerThanMaxIsSkipped() {
        StringBuilder sb = new StringBuilder();
        utils.appendIfFits(sb, "a".repeat(45));
        assertEquals("", sb.toString());
    }

    @Test
    void appendIfFitsTokenReachingExactlyMaxWithSeparatorFits() {
        StringBuilder sb = new StringBuilder("abc");
        utils.appendIfFits(sb, "b".repeat(40));
        assertEquals(44, sb.length());
    }

    @Test
    void appendIfFitsTokenExceedingMaxByOneWithSeparatorIsSkipped() {
        StringBuilder sb = new StringBuilder("abc");
        utils.appendIfFits(sb, "b".repeat(41));
        assertEquals("abc", sb.toString());
    }

    @Test
    void appendIfFitsSkipsLongTokenButAcceptsLaterShorterOne() {
        StringBuilder sb = new StringBuilder("VIA");
        utils.appendIfFits(sb, "x".repeat(50));
        utils.appendIfFits(sb, "10");
        assertEquals("VIA 10", sb.toString());
    }

    @Test
    void constructHouseNumberWithNumberAndLetterUsesSlash() {
        assertEquals("70/A", utils.constructHouseNumber("70", "A"));
    }

    @Test
    void constructHouseNumberWithOnlyNumber() {
        assertEquals("70", utils.constructHouseNumber("70", ""));
    }

    @Test
    void constructHouseNumberWithOnlyLetter() {
        assertEquals("A", utils.constructHouseNumber("", "A"));
    }

    @Test
    void constructHouseNumberWithEmptyValues() {
        assertEquals("", utils.constructHouseNumber("", ""));
    }
}


