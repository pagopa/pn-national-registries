package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebClientResponseProblemDto {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("title")
    private String title;

    @JsonProperty("detail")
    private String detail;

}
