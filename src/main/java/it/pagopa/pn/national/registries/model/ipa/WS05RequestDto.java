package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WS05RequestDto {

    @JsonProperty("COD_AMM")
    private String codiceAmministrazione;

    @JsonProperty("AUTH_ID")
    private String authId;

}
