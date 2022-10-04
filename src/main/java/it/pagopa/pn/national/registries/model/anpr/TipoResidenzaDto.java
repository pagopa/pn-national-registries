package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoResidenzaDto {
    private String tipoIndirizzo;
    private String noteIndirizzo;
    private TipoIndirizzoDto indirizzo;
    private TipoLocalitaEstera1Dto localitaEstera;
    private String presso;
    private String dataDecorrenzaResidenza;

}
