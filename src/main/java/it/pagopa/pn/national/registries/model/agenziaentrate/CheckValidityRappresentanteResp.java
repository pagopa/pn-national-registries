package it.pagopa.pn.national.registries.model.agenziaentrate;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "checkValidityRappresentanteResp", propOrder = {
        "valido",
        "dettaglioEsito",
        "codiceRitorno"
})
@XmlRootElement
public class CheckValidityRappresentanteResp {

    protected Boolean valido;
    @XmlElement(required = true)
    protected String dettaglioEsito;
    @XmlElement(required = true)
    protected String codiceRitorno;
}
