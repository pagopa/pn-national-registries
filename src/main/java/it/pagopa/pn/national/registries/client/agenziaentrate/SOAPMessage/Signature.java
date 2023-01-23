package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Signature", propOrder = {
        "SignedInfo",
        "SignatureValue",
        "KeyInfo"})
public class Signature {
    @XmlElement(name = "SignedInfo")
    public SignedInfo SignedInfo;
    @XmlElement(name = "SignatureValue")
    public String SignatureValue;
    @XmlElement(name = "KeyInfo")
    public KeyInfo KeyInfo;
    @XmlAttribute(name = "xmlns")
    public String xmlns;
}
