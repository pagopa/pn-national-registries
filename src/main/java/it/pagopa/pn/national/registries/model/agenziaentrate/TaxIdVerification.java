package it.pagopa.pn.national.registries.model.agenziaentrate;

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

