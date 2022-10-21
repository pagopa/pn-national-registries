package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InadResponseKO {

    private String detail;

    @JsonAlias({"status","code"})
    private String code;

    @JsonAlias({"element","type"})
    private String element;
}
