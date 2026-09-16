package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum DimensionName {
    STATUS("Status"),
    REGISTRY("Registry"),
    DIGITAL_ADDRESS_SCOPE("DigitalAddressScope"),
    NOTIFICATION_SCOPE("NotificationScope");

    private final String value;

    DimensionName(String value) {
        this.value = value;
    }
}
