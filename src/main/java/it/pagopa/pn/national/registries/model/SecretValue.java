package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SecretValue {

    private JwtConfig jwtConfig;

    @JsonProperty("client_id")
    private String clientId;

    private String keyId;

}
