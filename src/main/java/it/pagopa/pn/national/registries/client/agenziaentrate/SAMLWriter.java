package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.soap11.impl.EnvelopeMarshaller;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.xmlsec.signature.*;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class SAMLWriter {

    //TODO: Gestione certificato X509
    //TODO: gestione eccezioni
    //TODO: recuperare secret corretto per AdE
    //TODO: impostare i valori validi per creare l'assenzione SAML, decidere se metterli in configurazione

    private static final String DEFAULT_ELEMENT_NAME = "DEFAULT_ELEMENT_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";
    private static final String ELEMENT_NAME = "ELEMENT_NAME";
    private final AnprSecretConfig anprSecretConfig;

    private final OpenSAMLUtils openSAMLUtils;

    public SAMLWriter(AnprSecretConfig anprSecretConfig, OpenSAMLUtils openSAMLUtils) {
        this.anprSecretConfig = anprSecretConfig;
        this.openSAMLUtils = openSAMLUtils;
    }

    public String getEnvelope() {
        String originalAssertionString = null;
        try {
            Assertion assertion = buildDefaultAssertion();
            Security security = openSAMLUtils.buildSAMLObject(Security.class, ELEMENT_NAME);
            security.getUnknownXMLObjects().add(assertion);

            Envelope envelope = openSAMLUtils.buildSAMLObject(Envelope.class, DEFAULT_ELEMENT_NAME);
            Header header = openSAMLUtils.buildSAMLObject(Header.class, DEFAULT_ELEMENT_NAME);
            header.getUnknownXMLObjects().add(security);
            envelope.setHeader(header);
            envelope.setBody(createBody());

            EnvelopeMarshaller marshaller = new EnvelopeMarshaller();
            Element plaintextElement = marshaller.marshall(envelope);
            if (assertion != null) {
                Signer.signObject(Objects.requireNonNull(assertion.getSignature()));
            }
            originalAssertionString = SerializeSupport.nodeToString(plaintextElement);

        } catch (MarshallingException | SignatureException e) {
            e.printStackTrace();
        }
        log.info("originalAssertionString: {}", originalAssertionString);
        return originalAssertionString;
    }

    private Body createBody() {
        //TODO: dare nomi consoni alle variabili
        //TODO: capire come rimuovere il name space
        Body body = openSAMLUtils.buildSAMLObject(Body.class, DEFAULT_ELEMENT_NAME);
        QName parentBody = new QName("http://www.w3.org/2001/XMLSchema", "checkValidityRappresentante", "anag");
        QName cfEnte = new QName("http://www.w3.org/2001/XMLSchema", "cfEnte", "anag");
        QName cfRappresentante = new QName("http://www.w3.org/2001/XMLSchema", "cfRappresentante", "anag");
        XSAny xsAny = openSAMLUtils.buildSAMLCustomObject(XSAny.class, TYPE_NAME, parentBody);
        XSAny xsAny2 = openSAMLUtils.buildSAMLCustomObject(XSAny.class, TYPE_NAME, cfRappresentante);
        xsAny2.setTextContent("1° elemento");
        XSAny xsAny3 = openSAMLUtils.buildSAMLCustomObject(XSAny.class, TYPE_NAME, cfEnte);
        xsAny3.setTextContent("2° elemento");
        xsAny.getUnknownXMLObjects().add(xsAny2);
        xsAny.getUnknownXMLObjects().add(xsAny3);
        body.getUnknownXMLObjects().add(xsAny);
        return body;
    }

    private Assertion buildDefaultAssertion() {
        try {

            Assertion assertion = openSAMLUtils.buildSAMLObject(Assertion.class, DEFAULT_ELEMENT_NAME);
            assertion.setVersion(SAMLVersion.VERSION_20);
            assertion.setID(UUID.randomUUID().toString()); //TODO: value
            assertion.setIssueInstant(Instant.now());
            assertion.setIssuer(getIssuer());
            assertion.setSignature(signAssertion());
            assertion.setSubject(getSubject());
            assertion.setConditions(getConditions());
            assertion.getAuthnStatements().add(getAuthStatements());
            assertion.getAttributeStatements().add(getAttributeStatements());

            return assertion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AttributeStatement getAttributeStatements() {
        AttributeStatement attrStatement = openSAMLUtils.buildSAMLObject(AttributeStatement.class, DEFAULT_ELEMENT_NAME);
        attrStatement.getAttributes().add(getAttribute("User", "User")); //TODO: value
        attrStatement.getAttributes().add(getAttribute("IP-User", "IP-User")); //TODO: value
        return attrStatement;
    }

    private Attribute getAttribute(String name, String value) {
        Attribute attribute = openSAMLUtils.buildSAMLObject(Attribute.class, DEFAULT_ELEMENT_NAME);
        attribute.setName(name);
        attribute.setNameFormat(Attribute.UNSPECIFIED);
        attribute.getAttributeValues().add(getAttributeValue(value));
        return attribute;
    }

    private XMLObject getAttributeValue(String value) {
        AttributeValue attributeValue = openSAMLUtils.buildSAMLObject(AttributeValue.class, DEFAULT_ELEMENT_NAME);
        attributeValue.setTextContent(value);
        return attributeValue;
    }

    private AuthnStatement getAuthStatements() {
        AuthnContextClassRef authContextClassRef = openSAMLUtils.buildSAMLObject(AuthnContextClassRef.class, DEFAULT_ELEMENT_NAME);
        authContextClassRef.setURI(AuthnContext.UNSPECIFIED_AUTHN_CTX);

        AuthnContext authContext = openSAMLUtils.buildSAMLObject(AuthnContext.class, DEFAULT_ELEMENT_NAME);
        authContext.setAuthnContextClassRef(authContextClassRef);

        AuthnStatement authnStatement = openSAMLUtils.buildSAMLObject(AuthnStatement.class, DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(Instant.now());
        authnStatement.setSessionNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES)); //TODO: amountToAdd
        authnStatement.setAuthnContext(authContext);

        return authnStatement;
    }

    private Conditions getConditions() {
        Conditions conditions = openSAMLUtils.buildSAMLObject(Conditions.class, DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(Instant.now());
        conditions.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES)); //TODO: amountToAdd
        return conditions;
    }

    private Subject getSubject() {
        Subject subject = openSAMLUtils.buildSAMLObject(Subject.class, DEFAULT_ELEMENT_NAME);

        NameID nameID = openSAMLUtils.buildSAMLObject(NameID.class, DEFAULT_ELEMENT_NAME);
        nameID.setFormat(NameIDType.UNSPECIFIED);
        nameID.setValue("Codice Ente");
        subject.setNameID(nameID);

        SubjectConfirmation subjectConfirmation = openSAMLUtils.buildSAMLObject(SubjectConfirmation.class, DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        SubjectConfirmationData csubjectConfirmationData = openSAMLUtils.buildSAMLObject(SubjectConfirmationData.class, DEFAULT_ELEMENT_NAME);
        csubjectConfirmationData.setNotBefore(Instant.now());
        csubjectConfirmationData.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES)); //TODO: amountToAdd

        subjectConfirmation.setSubjectConfirmationData(csubjectConfirmationData);

        return subject;
    }


    private Signature signAssertion() {
        Signature signature = openSAMLUtils.buildSAMLObject(Signature.class, DEFAULT_ELEMENT_NAME);
        signature.setKeyInfo(getKeyInfo());
        signature.setSigningCredential(getSigningCredential());
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        return signature;
    }

    private Credential getSigningCredential() {
        SSLData sslData = anprSecretConfig.getAnprIntegritySecret();
        return CredentialSupport.getSimpleCredential(getCertificate(sslData.getCert()), getPrivateKey(sslData.getKey()));
    }

    private KeyInfo getKeyInfo() {
        KeyInfo keyInfo = openSAMLUtils.buildSAMLObject(KeyInfo.class, DEFAULT_ELEMENT_NAME);
        keyInfo.getX509Datas().add(getX509Data());
        return keyInfo;
    }

    private X509Data getX509Data() {
        X509Data x509Data = openSAMLUtils.buildSAMLObject(X509Data.class, DEFAULT_ELEMENT_NAME);
        SSLData sslData = anprSecretConfig.getAnprIntegritySecret();
        x509Data.getX509Certificates().add(getCert(sslData.getCert()));
        x509Data.getX509IssuerSerials().add(getX509IssuerSerial());
        return x509Data;
    }

    private X509IssuerSerial getX509IssuerSerial() {
        X509IssuerSerial x509IssuerSerial = openSAMLUtils.buildSAMLObject(X509IssuerSerial.class, DEFAULT_ELEMENT_NAME);
        x509IssuerSerial.setX509IssuerName(getX509IssuerName());
        x509IssuerSerial.setX509SerialNumber(getX509SerialNumber());
        return x509IssuerSerial;
    }

    private X509SerialNumber getX509SerialNumber() {
        X509SerialNumber x509SerialNumber = openSAMLUtils.buildSAMLObject(X509SerialNumber.class, DEFAULT_ELEMENT_NAME);
        x509SerialNumber.setValue(BigInteger.valueOf(123)); //TODO: value
        return x509SerialNumber;
    }

    private X509IssuerName getX509IssuerName() {
        X509IssuerName x509IssuerName = openSAMLUtils.buildSAMLObject(X509IssuerName.class, DEFAULT_ELEMENT_NAME);
        x509IssuerName.setValue("CN=ANPR,OU=ANPR,O=ANPR,L=ANPR,ST=ANPR,C=IT"); //TODO: value
        return x509IssuerName;
    }


    private Issuer getIssuer() {
        Issuer issuer = openSAMLUtils.buildSAMLObject(Issuer.class, DEFAULT_ELEMENT_NAME);
        issuer.setValue("PagoPA"); //TODO: value
        return issuer;
    }

    protected X509Certificate getCert(String cert) {
        X509Certificate certificate = openSAMLUtils.buildSAMLObject(X509Certificate.class, DEFAULT_ELEMENT_NAME);
        certificate.setValue(cert);
        return certificate;
    }

    protected PrivateKey getPrivateKey(String key) {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(key));
        try {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(encodedKeySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    protected java.security.cert.X509Certificate getCertificate(String cert) {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(cert));
        try {
            CertificateFactory kf = CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) kf.generateCertificate(is);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

}