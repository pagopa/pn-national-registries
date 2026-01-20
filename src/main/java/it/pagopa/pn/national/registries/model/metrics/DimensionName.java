package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum DimensionName {
    BATCH_TYPE("BatchType"),
    SERVICE_NAME("ServiceName"),
    SERVICE_OPERATION("ServiceOperation");

    private final String value;

    DimensionName(String value) {
        this.value = value;
    }
}
