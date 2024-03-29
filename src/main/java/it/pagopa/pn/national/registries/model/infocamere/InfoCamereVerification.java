package it.pagopa.pn.national.registries.model.infocamere;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoCamereVerification extends InfoCamereCommonError {

    @JsonProperty("dataOraEstrazione")
    private String dateTimeExtraction;

    @JsonProperty("cfPersona")
    private String taxId;

    @JsonProperty("cfImpresa")
    private String vatNumber;

    @JsonProperty("esitoVerifica")
    private String verificationResult;
}
