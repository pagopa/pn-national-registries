package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubjectConfirmation", namespace = "saml2", propOrder = {
        "SubjectConfirmationData"})
public class SubjectConfirmation {
    @XmlElement(name = "SubjectConfirmationData", namespace = "saml2")
    public SubjectConfirmationData SubjectConfirmationData;
    @XmlAttribute(name = "Method")
    public String Method;
}
