package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JwtConfig {

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("aud")
    private String audience;

    private String kid;
    private String purposeId;

}
