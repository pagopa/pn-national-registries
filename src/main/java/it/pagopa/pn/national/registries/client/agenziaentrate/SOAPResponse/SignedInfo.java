package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignedInfo", propOrder = {
        "CanonicalizationMethod",
        "SignatureMethod",
        "Reference"})
public class SignedInfo {
    @XmlElement(name = "CanonicalizationMethod")
    public CanonicalizationMethod CanonicalizationMethod;
    @XmlElement(name = "SignatureMethod")
    public SignatureMethod SignatureMethod;
    @XmlElement(name = "Reference")
    public Reference Reference;
}
