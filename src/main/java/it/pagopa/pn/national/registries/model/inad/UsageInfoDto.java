package it.pagopa.pn.national.registries.model.inad;

import lombok.Data;

import java.util.Date;

/**
 * Modello di risposta Info Utilizzo per singolo Domicilio Digitale
 */
@Data
public class UsageInfoDto {
  private MotivationTerminationDto motivazione;
  private Date dateEndValidity;

}

