package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignedHeaders {

    @JsonProperty("digest")
    private String digest;

    @JsonProperty("content-type")
    private String contentType;

    @JsonProperty("content-encoding")
    private String contentEncoding;
}
