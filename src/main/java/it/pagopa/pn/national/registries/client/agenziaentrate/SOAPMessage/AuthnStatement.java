package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AuthnStatement", namespace = "saml2", propOrder = {
        "AuthnContext"})
public class AuthnStatement {
    @XmlElement(name = "AuthnContext", namespace = "saml2")
    public AuthnContext AuthnContext;
    @XmlAttribute(name = "AuthnInstant")
    public Date AuthnInstant;
    @XmlAttribute(name = "SessionNotOnOrAfter")
    public Date SessionNotOnOrAfter;
}
