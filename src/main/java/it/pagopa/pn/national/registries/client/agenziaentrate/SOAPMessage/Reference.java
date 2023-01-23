package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Reference", propOrder = {
        "Transforms",
        "DigestMethod",
        "DigestValue"})
public class Reference {
    @XmlElement(name = "Transforms")
    public Transforms Transforms;
    @XmlElement(name = "DigestMethod")

    public DigestMethod DigestMethod;
    @XmlElement(name = "DigestValue")

    public String DigestValue;

    @XmlAttribute(name = "URI")
    public String URI;
}
