package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XMLWriterTest {

    @Test
    void getEnvelopeWithoutAssertionContainsBodyWithTaxIds() {
        SAMLAssertionWriter samlAssertionWriter = mock(SAMLAssertionWriter.class);
        when(samlAssertionWriter.buildDefaultAssertion()).thenReturn(null);
        XMLWriter xmlWriter = new XMLWriter(new OpenSAMLUtils(), samlAssertionWriter);

        String envelope = xmlWriter.getEnvelope("RSSMRA80A01H501U", "12345678901");

        assertNotNull(envelope);
        assertTrue(envelope.contains("Envelope"));
        assertTrue(envelope.contains("Header"));
        assertTrue(envelope.contains("Body"));
        assertTrue(envelope.contains(XMLWriterConstant.CHECK_VALIDITY_RAPPRESENTANTE));
        assertTrue(envelope.contains(XMLWriterConstant.VERIFICA_RAPPRESENTANTE_NAMESPACE));
        assertTrue(envelope.contains(XMLWriterConstant.SOAP_ENV_NAMESPACE));
        assertTrue(envelope.contains(">RSSMRA80A01H501U<"));
        assertTrue(envelope.contains(">12345678901<"));
        verify(samlAssertionWriter).buildDefaultAssertion();
    }

    @Test
    void getEnvelopeWithAssertionWithoutSignatureThrows() {
        SAMLAssertionWriter samlAssertionWriter = mock(SAMLAssertionWriter.class);
        Assertion assertion = mock(Assertion.class);
        when(assertion.getSignature()).thenReturn(null);
        when(samlAssertionWriter.buildDefaultAssertion()).thenReturn(assertion);
        XMLWriter xmlWriter = new XMLWriter(new OpenSAMLUtils(), samlAssertionWriter);

        assertThrows(RuntimeException.class, () -> xmlWriter.getEnvelope("cfRappresentante", "cfEnte"));
    }
}

