package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Date;

@Data
@XmlRootElement(name = "Envelope", namespace = "soapenv")
@XmlAccessorType(XmlAccessType.FIELD)
public class Envelope {
    @XmlElement(name = "Header", namespace = "soapenv")
    public Header Header;
    @XmlElement(name = "Body", namespace = "soapenv")
    public Body Body;
    @XmlAttribute(name = "soapenv", namespace = "xmlns")
    public String soapenv;
    @XmlAttribute(name = "test", namespace = "xmlns")
    public String test;

    public Envelope() {
        this.Body = new Body();
        this.Header = new Header();
    }
}
