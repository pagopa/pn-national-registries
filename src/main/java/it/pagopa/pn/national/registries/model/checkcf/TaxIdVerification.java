package it.pagopa.pn.national.registries.model.checkcf;

import lombok.Data;

/**
 * VerificaCodiceFiscale
 */
@Data
public class TaxIdVerification {
  private String codiceFiscale;
  private Boolean valido;
  private String messaggio;
}

