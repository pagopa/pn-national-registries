package it.pagopa.pn.national.registries.model.pdnd;

import lombok.Data;

@Data
public class PdndResponseError {
    private String code;
    private String detail;
}
