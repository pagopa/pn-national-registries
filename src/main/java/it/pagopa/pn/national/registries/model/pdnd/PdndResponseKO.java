package it.pagopa.pn.national.registries.model.pdnd;

import lombok.Data;

import java.util.List;

@Data
public class PdndResponseKO {
    private Integer status;
    private String title;
    private String type;
    private List<PdndResponseError> errors;
}
