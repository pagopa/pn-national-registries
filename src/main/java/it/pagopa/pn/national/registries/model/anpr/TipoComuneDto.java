package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoComuneDto {
    private String nomeComune;
    private String codiceIstat;
    private String siglaProvinciaIstat;
    private String descrizioneLocalita;
}
