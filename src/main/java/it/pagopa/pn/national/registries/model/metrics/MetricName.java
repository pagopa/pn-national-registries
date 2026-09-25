package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    BATCH_REQUEST_CREATION("BATCH_REQUEST_CREATION"),
    BATCH("BATCH"),
    BATCH_CLOSURE_DURATION("BATCH_CLOSURE_DURATION"),
    BATCH_SIZE("BATCH_SIZE"),
    CF_REQUESTED("CF_REQUESTED"),
    CF_WITH_ADDRESS("CF_WITH_ADDRESS"),
    CF_WITH_ERROR("CF_WITH_ERROR");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
