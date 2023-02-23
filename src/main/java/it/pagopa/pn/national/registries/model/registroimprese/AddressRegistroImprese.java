package it.pagopa.pn.national.registries.model.registroimprese;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereCommonError;
import lombok.Data;

@Data
public class AddressRegistroImprese extends InfoCamereCommonError {

    @JsonProperty("dataOraEstrazione")
    private String date;

    @JsonProperty("cf")
    private String taxId;

    @JsonProperty("indirizzoLocalizzazione")
    private LegalAddress address;
}
