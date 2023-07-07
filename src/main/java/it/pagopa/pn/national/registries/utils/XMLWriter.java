package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.soap11.impl.EnvelopeMarshaller;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Objects;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;
import static it.pagopa.pn.national.registries.utils.XMLWriterConstant.*;

@Component
@Slf4j
public class XMLWriter {

    private final OpenSAMLUtils openSAMLUtils;
    private final SAMLAssertionWriter samlAssertionWriter;

    public XMLWriter(OpenSAMLUtils openSAMLUtils,
                     SAMLAssertionWriter samlAssertionWriter) {
        this.openSAMLUtils = openSAMLUtils;
        this.samlAssertionWriter = samlAssertionWriter;
    }

    public String getEnvelope(String cfRappresentante, String cfEnte) {
        String xmlSoapRequest;
        try {
            Assertion assertion = samlAssertionWriter.buildDefaultAssertion();
            Security security = (Security) openSAMLUtils.buildSAMLObject(Security.ELEMENT_NAME, null);
            security.getUnknownXMLObjects().add(assertion);

            Envelope envelope = (Envelope) openSAMLUtils.buildSAMLObject(Envelope.DEFAULT_ELEMENT_NAME, getSoapEnvelopeQName(ENVELOPE));
            envelope.getNamespaceManager().registerNamespaceDeclaration(new Namespace(VERIFICA_RAPPRESENTANTE_NAMESPACE, ANAG));
            Header header = (Header) openSAMLUtils.buildSAMLObject(Header.DEFAULT_ELEMENT_NAME, getSoapEnvelopeQName(HEADER));
            header.getUnknownXMLObjects().add(security);
            envelope.setHeader(header);
            envelope.setBody(createBody(cfRappresentante, cfEnte));

            EnvelopeMarshaller marshaller = new EnvelopeMarshaller();
            Element plaintextElement = marshaller.marshall(envelope);
            if (assertion != null) {
                Signer.signObject(Objects.requireNonNull(assertion.getSignature()));
            }
            xmlSoapRequest = SerializeSupport.nodeToString(plaintextElement);

        } catch (MarshallingException | SignatureException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADE_LEGAL_CREATE_SOAP, ERROR_CODE_ADE_LEGAL_CREATE_SOAP, e);
        }
        log.debug("xmlSoapRequest: {}", xmlSoapRequest);
        return xmlSoapRequest;
    }

    private Body createBody(String cfRappresentante, String cfEnte) {
        Body body = (Body) openSAMLUtils.buildSAMLObject(Body.DEFAULT_ELEMENT_NAME, getSoapEnvelopeQName(BODY));

        XSAny checkValidityRappresentanteXmlSchema = (XSAny) openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, getXMLSchemaQName(CHECK_VALIDITY_RAPPRESENTANTE));

        XSAny cfRappresentanteXmlSchema = (XSAny) openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, getXMLSchemaQName(CF_RAPPRESENTANTE));
        cfRappresentanteXmlSchema.setTextContent(cfRappresentante);

        XSAny cfEnteXmlSchema = (XSAny) openSAMLUtils.buildSAMLObject(XSAny.TYPE_NAME, getXMLSchemaQName(CF_ENTE));
        cfEnteXmlSchema.setTextContent(cfEnte);

        checkValidityRappresentanteXmlSchema.getUnknownXMLObjects().add(cfRappresentanteXmlSchema);
        checkValidityRappresentanteXmlSchema.getUnknownXMLObjects().add(cfEnteXmlSchema);

        body.getUnknownXMLObjects().add(checkValidityRappresentanteXmlSchema);

        return body;
    }

    private QName getSoapEnvelopeQName(String localPart) {
        return new QName(SOAP_ENV_NAMESPACE, localPart, SOAP_ENV);
    }

    private QName getXMLSchemaQName(String localPart) {
        return new QName(VERIFICA_RAPPRESENTANTE_NAMESPACE, localPart, ANAG);
    }
}