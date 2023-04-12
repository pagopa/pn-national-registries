package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
public class NationalRegistriesProblem {

    private static final long serialVersionUID = 1L;

    @JsonProperty("status")
    private Integer status;
    @JsonProperty("title")
    private String title;
    @JsonProperty("detail")
    private String detail;
    @JsonProperty("traceId")
    private String traceId;
    @JsonProperty("timestamp")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timestamp;
    @JsonProperty("errors")
    private Object errors;

}
