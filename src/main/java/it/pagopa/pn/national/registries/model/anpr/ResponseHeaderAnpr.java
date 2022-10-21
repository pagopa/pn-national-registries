package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseHeaderAnpr {

    @JsonAlias({"clientOperationId","idOperazioneClient"})
    private String clientOperationId;
}
