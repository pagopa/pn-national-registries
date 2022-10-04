package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoIndirizzoEsteroDto {
    private String cap;
    private TipoDatoLocalitaEsteraDto localita;
}
