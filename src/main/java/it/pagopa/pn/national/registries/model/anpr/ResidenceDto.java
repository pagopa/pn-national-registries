package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResidenceDto {
    private String tipoIndirizzo;
    private String noteIndirizzo;
    private AddressDto indirizzo;
    private ForeignLocation1Dto localitaEstera;
    private String presso;
    private String dataDecorrenzaResidenza;

}
