package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseKO {

    @JsonAlias({"errorsList","listaErrori"})
    private List<ErrorListAnpr> errorsList;

    @JsonAlias({"anprOperationId","idOperazioneANPR"})
    private String anprOperationId;
}
