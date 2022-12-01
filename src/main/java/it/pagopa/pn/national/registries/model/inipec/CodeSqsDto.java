package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class CodeSqsDto {
    private String correlationId;
    private String taxId;
    private DigitalAddress primaryDigitalAddress;
    private PhysicalAddress primaryPhysicalAddress;
    private List<DigitalAddress> secondaryDigitalAddresses;
    private List<PhysicalAddress> secondaryPhysicalAddresses;
}
