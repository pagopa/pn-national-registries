package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestDateE002Dto {
    private String schedaAnagraficaRichiesta;
    private String dataRiferimentoRichiesta;
    private List<String> datiAnagraficiRichiesti = null;
    private String motivoRichiesta;
    private String casoUso;
}
