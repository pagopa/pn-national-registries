package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnprErrors {

    @JsonAlias({"errorsCode","codiceErroreAnomalia"})
    private String errorCode;

    @JsonAlias({"errorText","testoErroreAnomalia"})
    private String errorText;

    @JsonAlias({"errorType","tipoErroreAnomalia"})
    private String errorType;

}
