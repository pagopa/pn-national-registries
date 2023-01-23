package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NameID", namespace = "saml2")
public class NameID {
    @XmlAttribute(name = "Format")
    public String Format;
    @XmlValue
    public String text;
}
