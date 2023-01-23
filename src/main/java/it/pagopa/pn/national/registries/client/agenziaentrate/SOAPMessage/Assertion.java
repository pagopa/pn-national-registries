package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)

@XmlType(name = "Assertion", namespace = "saml2", propOrder = {
        "Issuer",
        "Signature",
        "Subject",
        "Conditions",
        "AuthnStatement",
        "AttributeStatement"})
@AllArgsConstructor
public class Assertion {
    @XmlElement(name = "Issuer", namespace = "saml2")
    public String Issuer;
    @XmlElement(name = "Signature")
    public Signature Signature;
    @XmlElement(name = "Subject", namespace = "saml2")
    public Subject Subject;
    @XmlElement(name = "Conditions", namespace = "saml2")
    public Conditions Conditions;
    @XmlElement(name = "AuthnStatement", namespace = "saml2")
    public AuthnStatement AuthnStatement;
    @XmlElement(name = "AttributeStatement", namespace = "saml2")
    public AttributeStatement AttributeStatement;
    @XmlAttribute(name = "Version")
    public double Version;
    @XmlAttribute(name = "ID")
    public String ID;
    @XmlAttribute(name = "IssueInstant")
    public Date IssueInstant;
    @XmlAttribute(name = "saml2", namespace = "xmlns")
    public String saml2;
    @XmlAttribute(name = "soapenv", namespace = "xmlns")
    public String SOAP;

    public Assertion() {
        Issuer = "issuer";
        Signature = new Signature();
        Subject = new Subject();
        Conditions = new Conditions();
        AuthnStatement = new AuthnStatement();
        AttributeStatement = new AttributeStatement();


    }
}
