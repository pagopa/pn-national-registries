package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPRequest;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

public class SOAPRquestTemplate {
    public class CanonicalizationMethod {
        public String Algorithm;
    }

    public class SignatureMethod {
        public String Algorithm;
    }

    public class Transform {
        public String Algorithm;
    }

    public class Transforms {
        public List<Transform> Transform;
    }

    public class DigestMethod {
        public String Algorithm;
    }

    public class Reference {
        public Transforms Transforms;
        public DigestMethod DigestMethod;
        public String DigestValue;
        public String URI;
        public String text;
    }

    public class SignedInfo {
        public CanonicalizationMethod CanonicalizationMethod;
        public SignatureMethod SignatureMethod;
        public Reference Reference;
    }

    public class X509IssuerSerial {
        public String X509IssuerName;
        public double X509SerialNumber;
    }

    public class X509Data {
        public String X509Certificate;
        public X509IssuerSerial X509IssuerSerial;
    }

    public class KeyInfo {
        public X509Data X509Data;
    }

    public class Signature {
        public SignedInfo SignedInfo;
        public String SignatureValue;
        public KeyInfo KeyInfo;
        public String xmlns;
        public String text;
    }

    public class NameID {
        public String Format;
        public String text;
    }

    public class SubjectConfirmationData {
        public Date NotBefore;
        public Date NotOnOrAfter;
    }

    public class SubjectConfirmation {
        public SubjectConfirmationData SubjectConfirmationData;
        public String Method;
    }

    public class Subject {
        public NameID NameID;
        public SubjectConfirmation SubjectConfirmation;
    }

    public class Conditions {
        public Date NotBefore;
        public Date NotOnOrAfter;
    }

    public class AuthnContext {
        public String AuthnContextClassRef;
    }

    public class AuthnStatement {
        public AuthnContext AuthnContext;
        public Date AuthnInstant;
        public Date SessionNotOnOrAfter;
        public String text;
    }

    public class Attribute {
        public String AttributeValue;
        public String Name;
        public String NameFormat;
        public String text;
    }

    public class AttributeStatement {
        public List<Attribute> Attribute;
    }

    public class Assertion {
        public String Issuer;
        public Signature Signature;
        public Subject Subject;
        public Conditions Conditions;
        public AuthnStatement AuthnStatement;
        public AttributeStatement AttributeStatement;
        public double Version;
        public String ID;
        public Date IssueInstant;
        public String saml2;
        public String SOAP;
        public String text;
    }

    public class Security {
        public Assertion Assertion;
        public String wsse;
        public String text;
    }

    public class Header {
        public Security Security;
    }

    public class interrogazioneEsempio {
        public String codiceFiscale;
    }

    @Data
    public class Body {
        @Data
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "checkValidityRappresentanteType", propOrder = {
                "cfRappresentante",
                "cfEnte"
        })
        public class CheckValidityRappresentanteType {

            @XmlElement(required = true)
            protected String cfRappresentante;
            @XmlElement(required = true)
            protected String cfEnte;
        }

        private CheckValidityRappresentanteType checkValidityRappresentanteType;
    }

    public class Envelope {
        public Header Header;
        public Body Body;
        public String soapenv;
        public String test;
        public String text;
    }
}
