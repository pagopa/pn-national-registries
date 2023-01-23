package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AuthnContext", namespace = "saml2", propOrder = {
        "AuthnContextClassRef"})
public class AuthnContext {
    @XmlElement(name = "AuthnContextClassRef", namespace = "saml2")
    public String AuthnContextClassRef;
}
