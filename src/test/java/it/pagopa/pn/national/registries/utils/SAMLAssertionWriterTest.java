package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.xmlsec.signature.*;

import javax.security.auth.x500.X500Principal;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SAMLAssertionWriterTest {

    @Mock
    private OpenSAMLUtils openSAMLUtils;

    @Mock
    private X509CertificateUtils x509CertificateUtils;

    @Mock
    private AdeLegalSecretConfig adeLegalSecretConfig;

    @InjectMocks
    private SAMLAssertionWriter samlAssertionWriter;

    @Test
    void buildDefaultAssertion_shouldHandleRootTraceId() {
        try (MockedStatic<MDCUtils> mocked = mockStatic(MDCUtils.class)) {
            mocked.when(MDCUtils::retrieveMDCContextMap).thenReturn(new HashMap<>());
            MDCUtils.retrieveMDCContextMap().put("rootTraceId", "12345");

            Assertion assertion = mock(Assertion.class);
            when(assertion.getNamespaceManager()).thenReturn(mock(NamespaceManager.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(Assertion.DEFAULT_ELEMENT_NAME, null)).thenReturn(assertion);
            Mockito.when(openSAMLUtils.buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(Issuer.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(Signature.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(Signature.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(KeyInfo.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(X509Data.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(X509Data.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME, null))
                    .thenReturn(mock(org.opensaml.xmlsec.signature.X509Certificate.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(X509IssuerName.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(X509IssuerName.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(X509IssuerSerial.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(X509IssuerSerial.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(X509SerialNumber.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(X509SerialNumber.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(NameID.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(NameID.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(Subject.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(Subject.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(SubjectConfirmation.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(SubjectConfirmationData.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(Conditions.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(Conditions.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(AuthnContextClassRef.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(AuthnContext.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(AuthnContext.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(AuthnStatement.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(AttributeStatement.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(Attribute.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(Attribute.class));
            Mockito.when(openSAMLUtils.buildSAMLObject(AttributeValue.DEFAULT_ELEMENT_NAME, null)).thenReturn(mock(AttributeValue.class));

            X509Certificate cert = mock(X509Certificate.class);
            X500Principal x500Principal = mock(X500Principal.class);
            when(x500Principal.getName(X500Principal.RFC1779)).thenReturn("CN=subject");
            when(cert.getIssuerX500Principal()).thenReturn(x500Principal);
            when(x509CertificateUtils.loadCertificate(any())).thenReturn(cert);
            SSLData sslData = new SSLData();
            sslData.setCert(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
            when(x509CertificateUtils.getKeyAndCertificate(any())).thenReturn(sslData);
            when(x509CertificateUtils.getPrivateKey(any())).thenReturn(mock(PrivateKey.class));

            Assertion response = samlAssertionWriter.buildDefaultAssertion();
            assertNotNull(response);
        }catch (Exception e){
            assertFalse(true);
        }
    }
}