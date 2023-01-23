package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Body", namespace = "soapenv", propOrder = {
        "CheckValidityRappresentanteType"})
public class RequestBody {
    @XmlElement(name = "CheckValidityRappresentanteType")
    private CheckValidityRappresentanteType CheckValidityRappresentanteType;

    public RequestBody(CheckValidityRappresentanteType checkValidityRappresentanteType) {
        this.CheckValidityRappresentanteType = checkValidityRappresentanteType;
    }
}
