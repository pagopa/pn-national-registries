package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Body", namespace = "soapenv", propOrder = {
        "CheckValidityRappresentanteRespType"})
public class ResponseBody {
    @XmlElement(name = "CheckValidityRappresentanteRespType")
    private CheckValidityRappresentanteRespType CheckValidityRappresentanteRespType;
    public ResponseBody(CheckValidityRappresentanteRespType checkValidityRappresentanteRespType) {
        this.CheckValidityRappresentanteRespType = checkValidityRappresentanteRespType;
    }
}
