package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnprResponseHeader {

    @JsonAlias({"recipientCode","codDestinatario"})
    private String recipientCode;

    @JsonAlias({"senderCode","codMittente"})
    private String senderCode;

    @JsonAlias({"result","esitoOperazione"})
    private String result;

    @JsonAlias({"anprOperationId","idOperazioneANPR"})
    private String anprOperationId;

    @JsonAlias({"clientOperationId","idOperazioneClient"})
    private String clientOperationId;
}
