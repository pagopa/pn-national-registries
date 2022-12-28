package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "X509Data", propOrder = {
        "X509Certificate",
        "X509IssuerSerial"})
public class X509Data {
    @XmlElement(name = "X509Certificate")
    public String X509Certificate;
    @XmlElement(name = "X509IssuerSerial")
    public X509IssuerSerial X509IssuerSerial;
}
