package it.pagopa.pn.national.registries.client.agenziaentrate.request;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "checkValidityRappresentanteRespType", propOrder = {
        "valido",
        "dettaglioEsito",
        "codiceRitorno"
})
public class SoapEnvelope {

    protected Boolean valido;
    @XmlElement(required = true)
    protected String dettaglioEsito;
    @XmlElement(required = true)
    protected String codiceRitorno;
}
