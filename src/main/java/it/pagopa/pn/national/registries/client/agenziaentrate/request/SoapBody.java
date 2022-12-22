package it.pagopa.pn.national.registries.client.agenziaentrate.request;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlRootElement(name = "soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapBody {
    @XmlElement(name = "checkValidityRappresentanteRespType")
    private SoapEnvelope body;
}
