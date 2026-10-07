package it.pagopa.pn.national.registries.model.agenziaentrate;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "checkValidityRappresentanteResp", namespace = "http://anagrafica.verifica.rappresentante.ente")
public class CheckValidityRappresentanteResp {
    @XmlElement(namespace = "http://anagrafica.verifica.rappresentante.ente")
    public Boolean valido;
    @XmlElement(namespace = "http://anagrafica.verifica.rappresentante.ente")
    public String dettaglioEsito;
    @XmlElement(namespace = "http://anagrafica.verifica.rappresentante.ente")
    public String codiceRitorno;

}
