package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataWS23Dto {

    @JsonProperty("domicilio_digitale")
    private String domicilioDigitale;

    @JsonProperty("tipo")
    private String type;

    @JsonProperty("cod_ente")
    private String codEnte;

    @JsonProperty("denominazione")
    private String denominazione;
}
