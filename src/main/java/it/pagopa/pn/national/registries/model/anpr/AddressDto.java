package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDto {
    private String cap;
    private MunicipalityDto comune;
    private String frazione;
    private ToponymDto toponimo;
    private StreetNumberDto numeroCivico;
}
