package it.pagopa.pn.national.registries.client.agenziaentrate;

import lombok.Data;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SAMLWriter{
    public String getAssertion() {
        String originalAssertionString = null;
        try {
            Assertion assertion = SAMLWriter.buildDefaultAssertion();
            AssertionMarshaller marshaller = new AssertionMarshaller();
            Element plaintextElement = marshaller.marshall(assertion);
            originalAssertionString = XMLHelper.nodeToString(plaintextElement);

            // TODO: now you can also add encryption....

        } catch (MarshallingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "Assertion String: " + originalAssertionString;
    }

    private static XMLObjectBuilderFactory builderFactory;

    public static XMLObjectBuilderFactory getSAMLBuilder() throws ConfigurationException {

        if(builderFactory == null){
            // OpenSAML 2.3
            DefaultBootstrap.bootstrap();
            builderFactory = Configuration.getBuilderFactory();
        }

        return builderFactory;
    }

    @SuppressWarnings("rawtypes")
    public static Attribute buildStringAttribute(String name, String value, XMLObjectBuilderFactory builderFactory) throws ConfigurationException{
        SAMLObjectBuilder attrBuilder = (SAMLObjectBuilder) getSAMLBuilder().getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
        Attribute attrFirstName = (Attribute) attrBuilder.buildObject();
        attrFirstName.setName(name);

        // Set custom Attributes
        XMLObjectBuilder stringBuilder = getSAMLBuilder().getBuilder(XSString.TYPE_NAME);
        XSString attrValueFirstName = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        attrValueFirstName.setValue(value);

        attrFirstName.getAttributeValues().add(attrValueFirstName);
        return attrFirstName;
    }

    @SuppressWarnings("rawtypes")
    public static Assertion buildDefaultAssertion(){
        try {
            // Create the NameIdentifier
            SAMLObjectBuilder nameIdBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(NameID.DEFAULT_ELEMENT_NAME);
            NameID nameId = (NameID) nameIdBuilder.buildObject();
            nameId.setValue("Codice Ente");
            nameId.setFormat(NameID.UNSPECIFIED);

            // Create the SubjectConfirmation
            SAMLObjectBuilder confirmationMethodBuilder = (SAMLObjectBuilder)  SAMLWriter.getSAMLBuilder().getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
            SubjectConfirmationData confirmationMethod = (SubjectConfirmationData) confirmationMethodBuilder.buildObject();
            DateTime now = new DateTime();
            confirmationMethod.setNotBefore(now);
            confirmationMethod.setNotOnOrAfter(now.plusMinutes(10));

            SAMLObjectBuilder subjectConfirmationBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
            SubjectConfirmation subjectConfirmation = (SubjectConfirmation) subjectConfirmationBuilder.buildObject();
            subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);

            // Create the Subject
            SAMLObjectBuilder subjectBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(Subject.DEFAULT_ELEMENT_NAME);
            Subject subject = (Subject) subjectBuilder.buildObject();

            subject.setNameID(nameId);
            subject.getSubjectConfirmations().add(subjectConfirmation);

            //Create the Conditions
            SAMLObjectBuilder conditionsBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
            Conditions conditions = (Conditions) conditionsBuilder.buildObject();
            conditions.setNotBefore(now);
            conditions.setNotOnOrAfter(now.plusMinutes(10));

            // Create the AuthnContextClassRef
            SAMLObjectBuilder authContextClassRefBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
            AuthnContextClassRef authContextClassRef = (AuthnContextClassRef) authContextClassRefBuilder.buildObject();
            authContextClassRef.setAuthnContextClassRef(AuthnContext.UNSPECIFIED_AUTHN_CTX);

            // Create the AuthnContext
            SAMLObjectBuilder authContextBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME);
            AuthnContext authContext = (AuthnContext) authContextBuilder.buildObject();
            authContext.setAuthnContextClassRef(authContextClassRef);

            // Create Authentication Statement
            SAMLObjectBuilder authStatementBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME);
            AuthnStatement authnStatement = (AuthnStatement) authStatementBuilder.buildObject();
            authnStatement.setAuthnContext(authContext);

            // Builder Attributes
            SAMLObjectBuilder attrStatementBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
            AttributeStatement attrStatement = (AttributeStatement) attrStatementBuilder.buildObject();

            // Create the attribute statement
            Map attributes = new HashMap();

            attributes.put("User", "User");
            attributes.put("IP-User","IP-User");

            if(attributes != null){
                Iterator keySet = attributes.keySet().iterator();
                while (keySet.hasNext()){
                    String key = keySet.next().toString();
                    String val = attributes.get(key).toString();
                    Attribute attr = buildStringAttribute(key, val, getSAMLBuilder());
                    attr.setNameFormat(Attribute.UNSPECIFIED);
                    attrStatement.getAttributes().add(attr);
                }
            }

            // Create Issuer
            SAMLObjectBuilder issuerBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
            Issuer issuer = (Issuer) issuerBuilder.buildObject();
            issuer.setValue("PagoPA");

            // Create the Signature
            SAMLObjectBuilder signatureBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(Signature.DEFAULT_ELEMENT_NAME);
            Signature signature = (Signature) signatureBuilder.buildObject();

            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            // Create the assertion
            SAMLObjectBuilder assertionBuilder = (SAMLObjectBuilder) SAMLWriter.getSAMLBuilder().getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
            Assertion assertion = (Assertion) assertionBuilder.buildObject();

            assertion.setVersion(SAMLVersion.VERSION_20);
            assertion.setID("AssertionID");
            assertion.setIssueInstant(now);

            assertion.setIssuer(issuer);
            //assertion.setSignature(signature);
            assertion.setSubject(subject);
            assertion.getAuthnStatements().add(authnStatement);
            assertion.getAttributeStatements().add(attrStatement);
            assertion.setConditions(conditions);

            return assertion;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Data
    public static class SAMLInputContainer{
        private Issuer issuer;
        private Signature signature;
        private Subject subject;
        private Conditions conditions;
        private AuthnStatement authStatement;
        private AttributeStatement attributeStatement;
    }
}