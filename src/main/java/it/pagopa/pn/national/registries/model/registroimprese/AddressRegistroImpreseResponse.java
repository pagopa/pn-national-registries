package it.pagopa.pn.national.registries.model.registroimprese;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.national.registries.model.infocamere.GenericErrorResponse;
import lombok.Data;

@Data
public class AddressRegistroImpreseResponse extends GenericErrorResponse {

    @JsonProperty("dataOraEstrazione")
    private String date;

    @JsonProperty("cf")
    private String taxId;

    @JsonProperty("indirizzoLocalizzazione")
    private LegalAddress address;
}
