package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

class XMLWriterConstantTest {
    /**
     * Method under test: default or parameterless constructor of {@link XMLWriterConstant}
     */
    @Test
    void testConstructor() {
        XMLWriterConstant xmlWriterConstant = new XMLWriterConstant();
        assertNotNull(xmlWriterConstant.SOAP_ENV_NAMESPACE);
    }
}

