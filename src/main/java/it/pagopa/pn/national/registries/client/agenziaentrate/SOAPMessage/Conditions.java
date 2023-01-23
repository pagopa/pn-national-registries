package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Conditions", namespace = "saml2")
public class Conditions {
    @XmlAttribute(name = "NotBefore")
    public Date NotBefore;
    @XmlAttribute(name = "NotOnOrAfter")
    public Date NotOnOrAfter;
}
