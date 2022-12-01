package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

@Data
public class PhysicalAddress {
    private String at;
    private String address;
    private String addressDetails;
    private String zip;
    private String municipality;
    private String municipalityDetails;
    private String province;
    private String foreignState;
}
