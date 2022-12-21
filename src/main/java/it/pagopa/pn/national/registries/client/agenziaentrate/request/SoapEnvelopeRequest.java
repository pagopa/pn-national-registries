package it.pagopa.pn.national.registries.client.agenziaentrate.request;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SoapEnvelopeRequest {

    private String header;
    private CheckValidityRappresentanteRespType body;

    public SoapEnvelopeRequest(String headerContent, CheckValidityRappresentanteRespType body) {
        this.header = headerContent;
        this.body = body;
    }

    public SoapEnvelopeRequest(CheckValidityRappresentanteRespType body) {
        this.body = body;
    }

    public String getHeader(){
        return header;
    }

    public CheckValidityRappresentanteRespType getBody() {
        return body;
    }
}
