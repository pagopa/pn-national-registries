package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Modello di risposta Info Utilizzo per singolo Domicilio Digitale
 */
@Data
public class UsageInfo {

  @JsonProperty("motivazione")
  private MotivationTerminationDto motivation;

  private String dateEndValidity;

}

