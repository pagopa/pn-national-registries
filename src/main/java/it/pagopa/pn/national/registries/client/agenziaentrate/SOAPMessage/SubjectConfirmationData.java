package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubjectConfirmationData", namespace = "saml2")
public class SubjectConfirmationData {
    @XmlAttribute(name = "NotBefore")
    public Date NotBefore;
    @XmlAttribute(name = "NotOnOrAfter")
    public Date NotOnOrAfter;
}
