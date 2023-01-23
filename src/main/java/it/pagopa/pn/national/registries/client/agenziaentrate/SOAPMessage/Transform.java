package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Transform")
public class Transform {
    @XmlAttribute(name = "Algorithm")
    public String Algorithm;
}
