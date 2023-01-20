package it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "checkValidityRappresentanteType", propOrder = {
        "cfRappresentante",
        "cfEnte"
})
public class CheckValidityRappresentanteType {

    @XmlElement(required = true)
    protected String cfRappresentante;
    @XmlElement(required = true)
    protected String cfEnte;

}
