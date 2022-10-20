package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnprResponseKO {

    @JsonAlias({"errorsList","listaErrori"})
    private List<AnprErrors> errorsList;
    @JsonAlias({"responseHeader","testataRisposta"})
    private AnprResponseHeader responseHeader;
}
