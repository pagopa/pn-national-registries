package it.pagopa.pn.national.registries.model.anpr;

import lombok.Data;

@Data
public class AnprResponseKO {
    private String code;
    private String detail;
    private String element;
    private String clientOperationId;
}
