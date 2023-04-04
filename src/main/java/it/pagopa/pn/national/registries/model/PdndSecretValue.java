package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdndSecretValue {

    private JwtConfig jwtConfig;

    @JsonProperty("client_id")
    private String clientId;

    private String keyId;

}
