package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "Envelope", namespace = "soapenv")
@XmlAccessorType(XmlAccessType.FIELD)
public class RequestEnvelope {
    @XmlElement(name = "Header", namespace = "soapenv")
    public Header Header;
    @XmlElement(name = "Body", namespace = "soapenv")
    public RequestBody Body;
    @XmlAttribute(name = "soapenv", namespace = "xmlns")
    public String soapenv;
    @XmlAttribute(name = "test", namespace = "xmlns")
    public String test;

    public RequestEnvelope(CheckValidityRappresentanteType checkValidityRappresentanteType) {
        this.Body = new RequestBody(checkValidityRappresentanteType);
    }
}
