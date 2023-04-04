package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpaSecret {
    @JsonProperty("AUTH_ID")
    private String authId;
}
