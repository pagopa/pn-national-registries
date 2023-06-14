package it.pagopa.pn.national.registries.model.infocamere;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoCamereInstitution {
    @JsonProperty("cfImpresa")
    private String businessTaxId;
    @JsonProperty("denominazione")
    private String businessName;
}
