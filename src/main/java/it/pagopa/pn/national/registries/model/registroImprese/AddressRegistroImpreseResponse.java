package it.pagopa.pn.national.registries.model.registroImprese;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AddressRegistroImpreseResponse {

    @JsonProperty("dataOraEstrazione")
    private String date;

    @JsonProperty("cf")
    private String taxId;

    @JsonProperty("indirizzoLocalizzazione")
    private LegalAddress address;
}
