package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class CodeSqsDto {
    private String correlationId;
    private String taxId;
    private PhysicalAddress physicalAddress;
    private List<DigitalAddress> digitalAddress;
    private String error;
}
