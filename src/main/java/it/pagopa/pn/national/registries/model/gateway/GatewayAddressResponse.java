package it.pagopa.pn.national.registries.model.gateway;

import it.pagopa.pn.national.registries.constant.GatewayError;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
public class GatewayAddressResponse {
    private String correlationId;

    private List<AddressInfo> addresses;

    @ToString.Exclude
    private String addressType;

    @Data
    public static class AddressInfo {
        private Integer recIndex;

        @ToString.Exclude
        private PhysicalAddress physicalAddress;

        @ToString.Exclude
        private List<DigitalAddress> digitalAddress;

        private GatewayError error;

        private HttpStatus errorStatus;

        private String registry;
    }
}
