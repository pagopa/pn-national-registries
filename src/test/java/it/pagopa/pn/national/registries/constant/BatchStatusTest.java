package it.pagopa.pn.national.registries.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchStatusTest {

    @Test
    void testFromValue() {
        assertEquals(BatchStatus.WORKED, BatchStatus.fromValue("WORKED"));
        assertThrows(IllegalArgumentException.class, () -> BatchStatus.fromValue("random"));
    }

    @Test
    void testToString() {
        assertEquals("NO_BATCH_ID", BatchStatus.NO_BATCH_ID.toString());
     }

}