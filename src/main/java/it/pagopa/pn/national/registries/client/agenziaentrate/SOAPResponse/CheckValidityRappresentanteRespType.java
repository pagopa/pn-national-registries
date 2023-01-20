package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CheckValidityRappresentanteRespType", propOrder = {
        "valido",
        "dettaglioEsito",
        "codiceRitorno"
})
public class CheckValidityRappresentanteRespType {

    protected Boolean valido;
    @XmlElement(required = true)
    protected String dettaglioEsito;
    @XmlElement(required = true)
    protected String codiceRitorno;
}