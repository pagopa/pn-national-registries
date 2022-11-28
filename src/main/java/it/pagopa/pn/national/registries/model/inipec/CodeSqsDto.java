package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.ArrayList;

@Data
public class CodeSqsDto {

    private String correlationId;
    private String taxId;
    private DigitalAddress primaryDigitalAddress;
    private PhysicalAddress primaryPhysicalAddress;
    private ArrayList<DigitalAddress> secondaryDigitalAddresses;
    private ArrayList<PhysicalAddress> secondaryPhysicalAddresses;
}
