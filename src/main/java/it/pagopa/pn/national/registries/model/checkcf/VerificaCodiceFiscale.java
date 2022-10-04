package it.pagopa.pn.national.registries.model.checkcf;

import lombok.Data;

/**
 * VerificaCodiceFiscale
 */
@Data
public class VerificaCodiceFiscale {
  private String codiceFiscale;
  private Boolean valido;
  private String messaggio;
}

