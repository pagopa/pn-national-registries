package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    EXTERNAL_SERVICE_INVOCATION("EXTERNAL_SERVICE_INVOCATION"),
    BATCH_CLOSURE_DURATION("BATCH_CLOSURE_DURATION"),
    BATCH_SIZE("BATCH_SIZE"),
    BATCH_ERROR("BATCH_RECORD_ERROR");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
