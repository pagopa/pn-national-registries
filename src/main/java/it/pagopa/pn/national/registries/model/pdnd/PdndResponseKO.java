package it.pagopa.pn.national.registries.model.pdnd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdndResponseKO {
    private Integer status;
    private String title;
    private String type;
    private List<PdndResponseError> errors;
}
