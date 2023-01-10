package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

@Data
public class DigitalAddress {

    private String type;
    private String address;
    private String recipient;

    public DigitalAddress(String type, String address, String recipient) {
        this.type = type;
        this.address = address;
        this.recipient = recipient;
    }
}
