package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Subject", namespace = "saml2", propOrder = {
        "NameID",
        "SubjectConfirmation"})
public class Subject {
    @XmlElement(name = "NameID", namespace = "saml2")
    public NameID NameID;
    @XmlElement(name = "SubjectConfirmation", namespace = "saml2")
    public SubjectConfirmation SubjectConfirmation;
}
