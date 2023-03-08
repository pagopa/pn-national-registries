package it.pagopa.pn.national.registries.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RuntimeJAXBExceptionTest {

    /**
     * Method under test: {@link RuntimeJAXBException#RuntimeJAXBException(String)}
     */
    @Test
    void testConstructor() {
        RuntimeJAXBException actualRuntimeJAXBException = new RuntimeJAXBException("An error occurred");
        assertNull(actualRuntimeJAXBException.getCause());
        assertEquals(0, actualRuntimeJAXBException.getSuppressed().length);
        assertEquals("An error occurred", actualRuntimeJAXBException.getMessage());
        assertEquals("An error occurred", actualRuntimeJAXBException.getLocalizedMessage());
    }
}
