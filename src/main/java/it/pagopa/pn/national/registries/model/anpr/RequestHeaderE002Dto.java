package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestHeaderE002Dto {
    private String idOperazioneClient;
    private String codMittente;
    private String codDestinatario;
    private String operazioneRichiesta;
    private String dataOraRichiesta;
    private String tipoOperazione;
    private String protocolloClient;
    private String dataProtocolloClient;
    private String tipoInvio;
    private String dataDecorrenza;
    private String dataDefinizionePratica;
    private String nomeApplicativo;
    private String versioneApplicativo;
    private String fornitoreApplicativo;
}
