package it.pagopa.pn.national.registries.model.agenziaentrate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxIdResponseKO {

    private String detail;

    @JsonAlias({"status","code"})
    private String code;

    @JsonAlias({"element","type"})
    private String element;
}
