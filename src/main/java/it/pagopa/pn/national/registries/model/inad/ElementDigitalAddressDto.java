package it.pagopa.pn.national.registries.model.inad;

import lombok.Data;

/**
 * Modello di risposta per singolo Domicilio Digitale
 */
@Data
public class ElementDigitalAddressDto {
  private String digitalAddress;
  private String practicedProfession;
  private UsageInfo usageInfo;
}

