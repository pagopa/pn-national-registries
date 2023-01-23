package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;

@Data
@NoArgsConstructor
@XmlRootElement(name = "Envelope", namespace = "soapenv")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResponseEnvelope {
    @XmlElement(name = "Header", namespace = "soapenv")
    public Header Header;
    @XmlElement(name = "Body", namespace = "soapenv")
    public ResponseBody Body;
    @XmlAttribute(name = "soapenv", namespace = "xmlns")
    public String soapenv;
    @XmlAttribute(name = "test", namespace = "xmlns")
    public String test;

    public ResponseEnvelope(ResponseBody responseBody) {
        this.Body = responseBody;
    }
    public ResponseEnvelope(CheckValidityRappresentanteRespType checkValidityRappresentanteRespType) {
        this.Body = new ResponseBody(checkValidityRappresentanteRespType);
    }

}
