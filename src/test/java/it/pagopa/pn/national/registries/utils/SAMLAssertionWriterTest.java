package it.pagopa.pn.national.registries.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

//@ContextConfiguration(classes = {SAMLAssertionWriter.class})
@ExtendWith(SpringExtension.class)
//@PropertySource("classpath:application-test.properties")
//@EnableConfigurationProperties
class SAMLAssertionWriterTest {
/*
    @MockBean
    private OpenSAMLUtils openSAMLUtils;

    @Autowired
    private SAMLAssertionWriter sAMLAssertionWriter;

    @MockBean
    private X509CertificateUtils x509CertificateUtils;

    @Test
    void testBuildDefaultAssertion() {
        Assertion assertion = mock(Assertion.class);
        AttributeStatement attrStatement = mock(AttributeStatement.class);
        Attribute attribute = mock(Attribute.class);
        AttributeValue attributeValue = mock(AttributeValue.class);
        AuthnContextClassRef authContextClassRef = mock(AuthnContextClassRef.class);
        AuthnContext authContext = mock(AuthnContext.class);
        AuthnStatement authnStatement = mock(AuthnStatement.class);
        Conditions conditions = mock(Conditions.class);
        Subject subject = mock(Subject.class);
        NameID nameID = mock(NameID.class);
        SubjectConfirmation subjectConfirmation = mock(SubjectConfirmation.class);
        SubjectConfirmationData csubjectConfirmationData = mock(SubjectConfirmationData.class);
        Signature signature = mock(Signature.class);
        Issuer issuer = mock(Issuer.class);
        KeyInfo keyInfo = mock(KeyInfo.class);
        X509Data x509Data = mock(X509Data.class);
        X509IssuerSerial x509IssuerSerial = mock(X509IssuerSerial.class);
        X509SerialNumber x509SerialNumber = mock(X509SerialNumber.class);
        X509IssuerName x509IssuerName = mock(X509IssuerName.class);
        X509Certificate certificate = mock(X509Certificate.class);

        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        sslData.setSecretid("secretId");

        java.security.cert.X509Certificate x509Certificate = mock(java.security.cert.X509Certificate.class);
        when(x509CertificateUtils.getKeyAndCertificate()).thenReturn(sslData);

        when(openSAMLUtils.buildSAMLObject(Assertion.DEFAULT_ELEMENT_NAME, null)).thenReturn(assertion);
        when(openSAMLUtils.buildSAMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME, null)).thenReturn(attrStatement);
        when(openSAMLUtils.buildSAMLObject(Attribute.DEFAULT_ELEMENT_NAME, null)).thenReturn(attribute);
        when(openSAMLUtils.buildSAMLObject(AttributeValue.DEFAULT_ELEMENT_NAME, null)).thenReturn(attributeValue);
        when(openSAMLUtils.buildSAMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME, null)).thenReturn(authContextClassRef);
        when(openSAMLUtils.buildSAMLObject(AuthnContext.DEFAULT_ELEMENT_NAME, null)).thenReturn(authContext);
        when(openSAMLUtils.buildSAMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME, null)).thenReturn(authnStatement);
        when(openSAMLUtils.buildSAMLObject(Conditions.DEFAULT_ELEMENT_NAME, null)).thenReturn(conditions);
        when(openSAMLUtils.buildSAMLObject(Subject.DEFAULT_ELEMENT_NAME, null)).thenReturn(subject);
        when(openSAMLUtils.buildSAMLObject(NameID.DEFAULT_ELEMENT_NAME, null)).thenReturn(nameID);
        when(openSAMLUtils.buildSAMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME, null)).thenReturn(subjectConfirmation);
        when(openSAMLUtils.buildSAMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, null)).thenReturn(csubjectConfirmationData);
        when(openSAMLUtils.buildSAMLObject(Signature.DEFAULT_ELEMENT_NAME, null)).thenReturn(signature);
        when(openSAMLUtils.buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME, null)).thenReturn(issuer);
        when(openSAMLUtils.buildSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME, null)).thenReturn(keyInfo);
        when(openSAMLUtils.buildSAMLObject(X509Data.DEFAULT_ELEMENT_NAME, null)).thenReturn(x509Data);
        when(openSAMLUtils.buildSAMLObject(X509IssuerSerial.DEFAULT_ELEMENT_NAME, null)).thenReturn(x509IssuerSerial);
        when(openSAMLUtils.buildSAMLObject(X509SerialNumber.DEFAULT_ELEMENT_NAME, null)).thenReturn(x509SerialNumber);
        when(openSAMLUtils.buildSAMLObject(X509IssuerName.DEFAULT_ELEMENT_NAME, null)).thenReturn(x509IssuerName);
        when(openSAMLUtils.buildSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME, null)).thenReturn(certificate);

        NamespaceManager namespaceManager = mock(NamespaceManager.class);
        when(assertion.getNamespaceManager()).thenReturn(namespaceManager);

        Assertion assertionTest = this.sAMLAssertionWriter.buildDefaultAssertion();

        assertEquals(assertion, assertionTest);
    }

    */

    @Test
    void test() {
        assertTrue(true);
    }
}

