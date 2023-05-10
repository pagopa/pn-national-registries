package it.pagopa.pn.national.registries.utils;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.util.AttributeMap;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.Signer;

import javax.xml.namespace.QName;

import java.util.List;

import static it.pagopa.pn.national.registries.utils.XMLWriterConstant.SOAP_ENV_NAMESPACE;
import static it.pagopa.pn.national.registries.utils.XMLWriterConstant.XML_SCHEMA_NAMESPACE;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class XMLWriterTest {
    @Mock
    private OpenSAMLUtils openSAMLUtils;


    @Mock
    private SAMLAssertionWriter samlAssertionWriter;


    @InjectMocks
    private XMLWriter xMLWriter;

    /**
     * Method under test: {@link XMLWriter#getEnvelope(String, String)}
     */
    @Test
    void testGetEnvelope() throws MarshallingException, InitializationException, XMLParserException {
        InitializationService.initialize();

        Assertion assertion = mock(Assertion.class);

        when(samlAssertionWriter.buildDefaultAssertion()).thenReturn(assertion);
        Security security = mock(Security.class);
        when(openSAMLUtils.buildSAMLObject(Security.ELEMENT_NAME, null)).thenReturn(security);
        when(assertion.getSignature()).thenReturn(mock(Signature.class));
        Envelope envelope = mock(Envelope.class);
        when(envelope.getElementQName()).thenReturn(new QName(SOAP_ENV_NAMESPACE, "Envelope", "soapenv"));
        when(envelope.getNamespaceManager()).thenReturn(mock(NamespaceManager.class));
        when(envelope.getUnknownAttributes()).thenReturn(mock(AttributeMap.class));
        when(openSAMLUtils.buildSAMLObject(Envelope.DEFAULT_ELEMENT_NAME, new QName(SOAP_ENV_NAMESPACE, "Envelope", "soapenv"))).thenReturn(envelope);
        Header header = mock(Header.class);
        when(openSAMLUtils.buildSAMLObject(Header.DEFAULT_ELEMENT_NAME, new QName(SOAP_ENV_NAMESPACE, "Header", "soapenv"))).thenReturn(header);
        Body body = mock(Body.class);
        when(openSAMLUtils.buildSAMLObject(Body.DEFAULT_ELEMENT_NAME, new QName(SOAP_ENV_NAMESPACE, "Body", "soapenv"))).thenReturn(body);

        XSAny checkValidityRappresentanteXmlSchema = mock(XSAny.class);
        when(openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, new QName(XML_SCHEMA_NAMESPACE, "checkValidityRappresentante", "anag"))).thenReturn(checkValidityRappresentanteXmlSchema);

        XSAny cfRappresentanteXmlSchema = mock(XSAny.class);
        when(openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, new QName(XML_SCHEMA_NAMESPACE, "cfRappresentante", "anag"))).thenReturn(cfRappresentanteXmlSchema);

        XSAny cfEnteXmlSchema = mock(XSAny.class);
        when(openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, new QName(XML_SCHEMA_NAMESPACE, "cfEnte", "anag"))).thenReturn(cfEnteXmlSchema);
        mockStatic(Signer.class);

        Assertions.assertDoesNotThrow(() -> xMLWriter.getEnvelope("cfRappresentante", "cfEnte"));

    }
}

