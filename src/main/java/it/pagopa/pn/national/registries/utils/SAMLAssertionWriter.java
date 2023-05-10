package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.signature.*;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static it.pagopa.pn.national.registries.utils.XMLWriterConstant.SOAP_ENV_NAMESPACE;

@Component
@Slf4j
public class SAMLAssertionWriter {

    private final OpenSAMLUtils openSAMLUtils;
    private final X509CertificateUtils x509CertificateUtils;
    private final AdeLegalSecretConfig adeLegalSecretConfig;

    public SAMLAssertionWriter(OpenSAMLUtils openSAMLUtils,
                               X509CertificateUtils x509CertificateUtils,
                               AdeLegalSecretConfig adeLegalSecretConfig) {
        this.openSAMLUtils = openSAMLUtils;
        this.x509CertificateUtils = x509CertificateUtils;
        this.adeLegalSecretConfig = adeLegalSecretConfig;
    }

    public Assertion buildDefaultAssertion() {
        Assertion assertion = (Assertion) openSAMLUtils.buildSAMLObject(Assertion.DEFAULT_ELEMENT_NAME, null);

        assertion.getNamespaceManager().registerNamespaceDeclaration(new Namespace(SOAP_ENV_NAMESPACE, "SOAP"));
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setID(UUID.randomUUID().toString());
        assertion.setIssueInstant(Instant.now());
        assertion.setIssuer(getIssuer());
        assertion.setSignature(signAssertion(assertion));
        assertion.setSubject(getSubject());
        assertion.setConditions(getConditions());
        assertion.getAuthnStatements().add(getAuthStatements());
        assertion.getAttributeStatements().add(getAttributeStatements());

        return assertion;
    }

    private AttributeStatement getAttributeStatements() {
        AttributeStatement attrStatement = (AttributeStatement) openSAMLUtils.buildSAMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME, null);
        attrStatement.getAttributes().add(getAttribute("User", "User")); //TODO: value
        attrStatement.getAttributes().add(getAttribute("IP-User", "IP-User")); //TODO: value
        return attrStatement;
    }

    private Attribute getAttribute(String name, String value) {
        Attribute attribute = (Attribute) openSAMLUtils.buildSAMLObject(Attribute.DEFAULT_ELEMENT_NAME, null);
        attribute.setName(name);
        attribute.setNameFormat(Attribute.UNSPECIFIED);
        attribute.getAttributeValues().add(getAttributeValue(value));
        return attribute;
    }

    private XMLObject getAttributeValue(String value) {
        AttributeValue attributeValue = (AttributeValue) openSAMLUtils.buildSAMLObject(AttributeValue.DEFAULT_ELEMENT_NAME, null);
        attributeValue.setTextContent(value);
        return attributeValue;
    }

    private AuthnStatement getAuthStatements() {
        AuthnContextClassRef authContextClassRef = (AuthnContextClassRef) openSAMLUtils.buildSAMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME, null);
        authContextClassRef.setURI(AuthnContext.UNSPECIFIED_AUTHN_CTX);

        AuthnContext authContext = (AuthnContext) openSAMLUtils.buildSAMLObject(AuthnContext.DEFAULT_ELEMENT_NAME, null);
        authContext.setAuthnContextClassRef(authContextClassRef);

        AuthnStatement authnStatement = (AuthnStatement) openSAMLUtils.buildSAMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME, null);
        authnStatement.setAuthnInstant(Instant.now());
        authnStatement.setSessionNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES));
        authnStatement.setAuthnContext(authContext);

        return authnStatement;
    }

    private Conditions getConditions() {
        Conditions conditions = (Conditions) openSAMLUtils.buildSAMLObject(Conditions.DEFAULT_ELEMENT_NAME, null);
        conditions.setNotBefore(Instant.now());
        conditions.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES));
        return conditions;
    }

    private Subject getSubject() {
        Subject subject = (Subject) openSAMLUtils.buildSAMLObject(Subject.DEFAULT_ELEMENT_NAME, null);

        NameID nameID = (NameID) openSAMLUtils.buildSAMLObject(NameID.DEFAULT_ELEMENT_NAME, null);
        nameID.setFormat(NameIDType.UNSPECIFIED);
        nameID.setValue("Codice Ente"); //TODO: NameID
        subject.setNameID(nameID);

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) openSAMLUtils.buildSAMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME, null);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        SubjectConfirmationData csubjectConfirmationData = (SubjectConfirmationData) openSAMLUtils.buildSAMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, null);
        csubjectConfirmationData.setNotBefore(Instant.now());
        csubjectConfirmationData.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.MINUTES));

        subjectConfirmation.setSubjectConfirmationData(csubjectConfirmationData);

        return subject;
    }


    private Signature signAssertion(Assertion assertion) {
        Signature signature = (Signature) openSAMLUtils.buildSAMLObject(Signature.DEFAULT_ELEMENT_NAME, null);

        SAMLObjectContentReference samlObjectContentReference = new SAMLObjectContentReference(assertion);
        samlObjectContentReference.setDigestAlgorithm(SignatureConstants.ALGO_ID_DIGEST_SHA1);
        signature.getContentReferences().add(samlObjectContentReference);

        signature.setKeyInfo(getKeyInfo());
        signature.setSigningCredential(getSigningCredential());
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        return signature;
    }

    private Credential getSigningCredential() {
        return CredentialSupport.getSimpleCredential(x509CertificateUtils.getCertificate(), x509CertificateUtils.getPrivateKey());
    }
    private Issuer getIssuer() {
        Issuer issuer = (Issuer) openSAMLUtils.buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME, null);
        issuer.setValue("PagoPA"); //TODO: value
        return issuer;
    }

    private KeyInfo getKeyInfo() {
        KeyInfo keyInfo = (KeyInfo) openSAMLUtils.buildSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME, null);
        keyInfo.getX509Datas().add(getX509Data());
        return keyInfo;
    }

    private X509Data getX509Data() {
        X509Data x509Data = (X509Data) openSAMLUtils.buildSAMLObject(X509Data.DEFAULT_ELEMENT_NAME, null);
        SSLData sslData = adeLegalSecretConfig.getAdeSecretConfig();
        x509Data.getX509Certificates().add(getX509Certificate(sslData.getCert()));
        x509Data.getX509IssuerSerials().add(getX509IssuerSerial());
        return x509Data;
    }

    private X509IssuerSerial getX509IssuerSerial() {
        X509IssuerSerial x509IssuerSerial = (X509IssuerSerial) openSAMLUtils.buildSAMLObject(X509IssuerSerial.DEFAULT_ELEMENT_NAME, null);
        x509IssuerSerial.setX509IssuerName(getX509IssuerName());
        x509IssuerSerial.setX509SerialNumber(getX509SerialNumber());
        return x509IssuerSerial;
    }

    private X509SerialNumber getX509SerialNumber() {
        X509SerialNumber x509SerialNumber = (X509SerialNumber) openSAMLUtils.buildSAMLObject(X509SerialNumber.DEFAULT_ELEMENT_NAME, null);
        x509SerialNumber.setValue(x509CertificateUtils.getSerialNumber());
        return x509SerialNumber;
    }

    private X509IssuerName getX509IssuerName() {
        X509IssuerName x509IssuerName = (X509IssuerName) openSAMLUtils.buildSAMLObject(X509IssuerName.DEFAULT_ELEMENT_NAME, null);
        x509IssuerName.setValue(x509CertificateUtils.getIssuerName());
        return x509IssuerName;
    }

    protected X509Certificate getX509Certificate(String cert) {
        X509Certificate certificate = (X509Certificate) openSAMLUtils.buildSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME, null);
        certificate.setValue(cert);
        return certificate;
    }
}