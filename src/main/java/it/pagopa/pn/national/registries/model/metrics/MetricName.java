package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    BATCH_REQUEST_INVOCATION("BATCH_REQUEST_INVOCATION"),
    BATCH_POLLING_INVOCATION("BATCH_POLLING_INVOCATION"),
    BATCH_CLOSURE_DURATION("BATCH_CLOSURE_DURATION"),
    SENT_BATCH_SIZE("SENT_BATCH_SIZE"),
    BATCH_KO("BATCH_KO");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
