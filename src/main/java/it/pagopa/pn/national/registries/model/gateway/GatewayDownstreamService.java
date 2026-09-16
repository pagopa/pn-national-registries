package it.pagopa.pn.national.registries.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GatewayDownstreamService {
    ANPR,
    REGISTRO_IMPRESE,
    INIPEC,
    INAD,
    IPA;

    @JsonCreator
    public static GatewayDownstreamService fromName(String name) {
        for (GatewayDownstreamService b : GatewayDownstreamService.values()) {
            if (b.name().equals(name)) {
                return b;
            }
        }
        log.warn("Unexpected value '" + name + "' for DigitalAddressRecipientType");
        return null;
    }
}
