package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoIndirizzoDto {
    private String cap;
    private TipoComuneDto comune;
    private String frazione;
    private TipoToponimoDto toponimo;
    private TipoNumeroCivicoDto numeroCivico;
}
