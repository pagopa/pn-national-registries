package it.pagopa.pn.national.registries.model.inipec;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class PecProfessionista {

    @JsonProperty("pecProfessionista")
    private String pec;

}
