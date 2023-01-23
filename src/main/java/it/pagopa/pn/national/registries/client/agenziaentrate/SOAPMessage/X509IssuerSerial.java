package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "X509IssuerSerial", propOrder = {
        "X509IssuerName",
        "X509SerialNumber"})
public class X509IssuerSerial {
    @XmlElement(name = "X509IssuerName")
    public String X509IssuerName;
    @XmlElement(name = "X509SerialNumber")
    public double X509SerialNumber;
}
