package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.model.SSLData;
import org.springframework.beans.factory.annotation.Value;
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

import javax.security.auth.x500.X500Principal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.utils.XMLWriterConstant.SOAP_ENV_NAMESPACE;

@Component
@Slf4j
public class SAMLAssertionWriter {

    static final Pattern pattern = Pattern.compile(".*Root=(.*);P.*");
    private final OpenSAMLUtils openSAMLUtils;
    private final X509CertificateUtils x509CertificateUtils;
    private final String clientId;
    private final String environmentType;
    private final String issuerName;

    public SAMLAssertionWriter(OpenSAMLUtils openSAMLUtils,
                               X509CertificateUtils x509CertificateUtils,
                               @Value("${pn.national.registries.ade.legal.name.id}") String clientId,
                               @Value("${pn.national.registries.environment.type}") String environmentType,
                               @Value("${pn.national.registries.issuer}") String issuerName) {
        this.openSAMLUtils = openSAMLUtils;
        this.x509CertificateUtils = x509CertificateUtils;
        this.clientId = clientId;
        this.environmentType = environmentType;
        this.issuerName = issuerName;
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
        String traceId = "unknown";
        String allTraceId  = MDCUtils.retrieveMDCContextMap().get(MDC_TRACE_ID_KEY);
        final Matcher matcher = pattern.matcher(allTraceId);
        if(matcher.find()) {
            traceId = matcher.group(1);
        }
        AttributeStatement attrStatement = (AttributeStatement) openSAMLUtils.buildSAMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME, null);
        attrStatement.getAttributes().add(getAttribute("User", traceId));
        attrStatement.getAttributes().add(getAttribute("IP-User", environmentType));
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
        authnStatement.setSessionNotOnOrAfter(Instant.now().plus(10, ChronoUnit.HOURS));
        authnStatement.setAuthnContext(authContext);

        return authnStatement;
    }

    private Conditions getConditions() {
        Conditions conditions = (Conditions) openSAMLUtils.buildSAMLObject(Conditions.DEFAULT_ELEMENT_NAME, null);
        conditions.setNotBefore(Instant.now());
        conditions.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.HOURS));
        return conditions;
    }

    private Subject getSubject() {
        Subject subject = (Subject) openSAMLUtils.buildSAMLObject(Subject.DEFAULT_ELEMENT_NAME, null);

        NameID nameID = (NameID) openSAMLUtils.buildSAMLObject(NameID.DEFAULT_ELEMENT_NAME, null);
        nameID.setFormat(NameIDType.UNSPECIFIED);
        nameID.setValue(clientId);
        subject.setNameID(nameID);

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) openSAMLUtils.buildSAMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME, null);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        SubjectConfirmationData csubjectConfirmationData = (SubjectConfirmationData) openSAMLUtils.buildSAMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, null);
        csubjectConfirmationData.setNotBefore(Instant.now());
        csubjectConfirmationData.setNotOnOrAfter(Instant.now().plus(10, ChronoUnit.HOURS));

        subjectConfirmation.setSubjectConfirmationData(csubjectConfirmationData);

        return subject;
    }


    private Signature signAssertion(Assertion assertion) {
        Signature signature = (Signature) openSAMLUtils.buildSAMLObject(Signature.DEFAULT_ELEMENT_NAME, null);

        SAMLObjectContentReference samlObjectContentReference = new SAMLObjectContentReference(assertion);
        samlObjectContentReference.setDigestAlgorithm(SignatureConstants.ALGO_ID_DIGEST_SHA1);
        signature.getContentReferences().add(samlObjectContentReference);
        SSLData sslData = x509CertificateUtils.getKeyAndCertificate();

        signature.setKeyInfo(getKeyInfo(sslData.getCert()));
        signature.setSigningCredential(getSigningCredential(sslData));
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        return signature;
    }

    private Credential getSigningCredential(SSLData sslData) {
        return CredentialSupport.getSimpleCredential(x509CertificateUtils.loadCertificate(sslData.getCert()), x509CertificateUtils.getPrivateKey(sslData.getSecretid()));
    }
    private Issuer getIssuer() {
        Issuer issuer = (Issuer) openSAMLUtils.buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME, null);
        issuer.setValue(environmentType + issuerName);
        return issuer;
    }

    private KeyInfo getKeyInfo(String cert) {
        KeyInfo keyInfo = (KeyInfo) openSAMLUtils.buildSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME, null);
        keyInfo.getX509Datas().add(getX509Data(cert));
        return keyInfo;
    }

    private X509Data getX509Data(String cert) {
        X509Data x509Data = (X509Data) openSAMLUtils.buildSAMLObject(X509Data.DEFAULT_ELEMENT_NAME, null);
        x509Data.getX509Certificates().add(getX509Certificate(cert));
        x509Data.getX509IssuerSerials().add(getX509IssuerSerial(cert));
        return x509Data;
    }

    private X509IssuerSerial getX509IssuerSerial(String cert) {
        X509IssuerSerial x509IssuerSerial = (X509IssuerSerial) openSAMLUtils.buildSAMLObject(X509IssuerSerial.DEFAULT_ELEMENT_NAME, null);
        x509IssuerSerial.setX509IssuerName(getX509IssuerName(cert));
        x509IssuerSerial.setX509SerialNumber(getX509SerialNumber(cert));
        return x509IssuerSerial;
    }

    private X509SerialNumber getX509SerialNumber(String cert) {
        X509SerialNumber x509SerialNumber = (X509SerialNumber) openSAMLUtils.buildSAMLObject(X509SerialNumber.DEFAULT_ELEMENT_NAME, null);
        x509SerialNumber.setValue(x509CertificateUtils.loadCertificate(cert).getSerialNumber());
        return x509SerialNumber;
    }

    private X509IssuerName getX509IssuerName(String cert) {
        X509IssuerName x509IssuerName = (X509IssuerName) openSAMLUtils.buildSAMLObject(X509IssuerName.DEFAULT_ELEMENT_NAME, null);
        x509IssuerName.setValue(x509CertificateUtils.loadCertificate(cert).getIssuerX500Principal().getName(X500Principal.RFC1779));
        return x509IssuerName;
    }

    protected X509Certificate getX509Certificate(String cert) {

        byte[] array = Base64.getDecoder().decode(cert);

        String str = new String(array, StandardCharsets.UTF_8);
        str = str.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .trim();

        X509Certificate certificate = (X509Certificate) openSAMLUtils.buildSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME, null);
        certificate.setValue(str);
        return certificate;
    }
}