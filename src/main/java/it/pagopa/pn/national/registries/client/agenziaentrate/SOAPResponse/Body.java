package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Body", namespace = "soapenv", propOrder = {
        "CheckValidityRappresentanteRespType"})
public class Body {
    @XmlElement(name = "CheckValidityRappresentanteRespType")
    public ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType CheckValidityRappresentanteRespType;

    public Body() {
       /* CheckValidityRappresentanteRespType = new CheckValidityRappresentanteRespType();
        CheckValidityRappresentanteRespType.setValido(true);
        CheckValidityRappresentanteRespType.setDettaglioEsito("testDetail");
        CheckValidityRappresentanteRespType.setCodiceRitorno("testCode");*/

    }
}
