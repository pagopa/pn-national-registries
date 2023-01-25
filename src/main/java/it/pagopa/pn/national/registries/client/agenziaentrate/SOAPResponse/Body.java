package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

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
    public it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse.CheckValidityRappresentanteRespType CheckValidityRappresentanteRespType;

    public Body() {
       /* CheckValidityRappresentanteRespType = new CheckValidityRappresentanteRespType();
        CheckValidityRappresentanteRespType.setValido(true);
        CheckValidityRappresentanteRespType.setDettaglioEsito("testDetail");
        CheckValidityRappresentanteRespType.setCodiceRitorno("testCode");*/

    }
}