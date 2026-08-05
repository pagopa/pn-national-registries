package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    BATCH_REQUEST_CREATION("BATCH_REQUEST_CREATION"),
    BATCH("BATCH"),
    BATCH_CLOSURE_DURATION("BATCH_CLOSURE_DURATION"),
    BATCH_SIZE("BATCH_SIZE");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
