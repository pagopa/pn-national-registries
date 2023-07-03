package it.pagopa.pn.national.registries.model.inipec;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
