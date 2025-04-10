package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void getMostRecentInstant_withNullList_returnsNull() {
        assertNull(Utils.getMostRecentInstant(null));
    }

    @Test
    void getMostRecentInstant_withEmptyList_returnsNull() {
        assertNull(Utils.getMostRecentInstant(List.of()));
    }

    @Test
    void getMostRecentInstant_withSingleElement_returnsElement() {
        Instant instant = Instant.now();
        assertEquals(instant, Utils.getMostRecentInstant(List.of(instant)));
    }

    @Test
    void getMostRecentInstant_withMultipleElements_returnsMostRecent() {
        Instant instant1 = Instant.parse("2023-01-01T00:00:00Z");
        Instant instant2 = Instant.parse("2023-02-01T00:00:00Z");
        Instant instant3 = Instant.parse("2023-03-01T00:00:00Z");
        assertEquals(instant3, Utils.getMostRecentInstant(List.of(instant1, instant2, instant3)));
    }

    @Test
    void getMostRecentInstant_withIdenticalElements_returnsElement() {
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        assertEquals(instant, Utils.getMostRecentInstant(List.of(instant, instant, instant)));
    }
}