package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Attribute", namespace = "saml2", propOrder = {
        "AttributeValue"})
public class Attribute {
    @XmlElement(name = "AttributeValue", namespace = "saml2")
    public String AttributeValue;
    @XmlAttribute(name = "Name")
    public String Name;
    @XmlAttribute(name = "NameFormat")
    public String NameFormat;
}