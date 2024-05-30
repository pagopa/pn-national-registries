package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClientCredentialsResponseDto {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private TokenTypeDto tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;
}

