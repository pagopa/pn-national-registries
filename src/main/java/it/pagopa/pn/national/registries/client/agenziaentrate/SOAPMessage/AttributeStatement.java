package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AttributeStatement", namespace = "saml2")
public class AttributeStatement {
    @XmlElement(name = "Attribute", namespace = "saml2")
    public List<Attribute> Attribute;

}
