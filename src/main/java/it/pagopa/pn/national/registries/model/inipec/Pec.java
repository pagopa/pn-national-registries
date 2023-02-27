package it.pagopa.pn.national.registries.model.inipec;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Pec {

    private String cf;

    private String pecImpresa;

    @JsonProperty("pecProfessionistas")
    private List<PecProfessionista> pecProfessionista;

}

