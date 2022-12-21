package it.pagopa.pn.national.registries.model.agenziaentrate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ADELegalResponseDto {

    @JsonProperty("valido")
    private boolean verificationResult;

    @JsonProperty("dettaglioEsito")
    private String resultDetail;

    @JsonProperty("codiceRitorno")
    private String resultCode;
}
