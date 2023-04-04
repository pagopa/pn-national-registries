package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultDto {

    @JsonProperty("cod_err")
    private int codError;

    @JsonProperty("desc_err")
    private String descError;

    @JsonProperty("num_items")
    private int numItems;
}
