package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Security", namespace = "wsse", propOrder = {
        "Assertion"})
public class Security {

    @XmlElement(name = "Assertion", namespace = "saml2")
    public Assertion Assertion;
    @XmlAttribute(name = "wsse", namespace = "xmlns")
    public String wsse;

    public Security() {
        Assertion = new Assertion();
        wsse = "wsse";
    }
}
