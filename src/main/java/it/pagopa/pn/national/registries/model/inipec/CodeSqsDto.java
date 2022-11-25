package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class CodeSqsDto {

    private String correlationId;
    private String cf;
    private String pecImpresa;
    private List<String> pecProfessionista;
    private String status;
    private String descrption;
}
