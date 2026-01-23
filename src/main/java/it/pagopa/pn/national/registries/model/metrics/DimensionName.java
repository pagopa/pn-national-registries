package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum DimensionName {
    BATCH_TYPE("BatchType");

    private final String value;

    DimensionName(String value) {
        this.value = value;
    }
}
