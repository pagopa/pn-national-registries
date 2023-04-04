package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WS23ResponseDto {

    @JsonProperty("result")
    private ResultDto result;

    @JsonProperty("data")
    private List<DataWS23Dto> data;
}