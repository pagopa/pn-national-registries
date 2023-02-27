package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Modello di risposta Info Utilizzo per singolo Domicilio Digitale
 */
@Data
public class UsageInfo {

  @JsonProperty("motivazione")
  private MotivationTerminationDto motivation;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private Date dateEndValidity;

}
