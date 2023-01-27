package it.pagopa.pn.national.registries.model.infocamere;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfocamereResponseKO {

    @JsonAlias({"status","code"})
    private String code;

    @JsonAlias({"element","error"})
    private String element;
}
