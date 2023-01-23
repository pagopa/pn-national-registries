package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Header", namespace = "soapenv", propOrder = {
        "Security"})
public class Header {
    @XmlElement(name = "Security", namespace = "wsse")
    public Security Security;

    public Header() {
        Security = new Security();
    }

}
