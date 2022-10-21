package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorListAnpr {

    @JsonAlias({"code","codiceErroreAnomalia"})
    private String code;

    @JsonAlias({"detail","testoErroreAnomalia"})
    private String detail;

    @JsonAlias({"element","tipoErroreAnomalia"})
    private String element;

}
