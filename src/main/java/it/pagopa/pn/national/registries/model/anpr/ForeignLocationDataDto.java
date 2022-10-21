package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForeignLocationDataDto {
  private String descrizioneLocalita;
  private String descrizioneStato;
  private String provinciaContea;
  private String codiceStato;
}
