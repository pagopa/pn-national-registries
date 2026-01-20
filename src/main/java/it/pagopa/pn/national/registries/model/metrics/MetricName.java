package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    INIPEC_REQUEST_INVOCATION("INIPEC_REQUEST_INVOCATION"),
    BATCH_CLOSURE_DURATION("BATCH_CLOSURE_DURATION"),
    SENT_BATCH_SIZE("SENT_BATCH_SIZE"),
    BATCH_ERROR("BATCH_RECORD_ERROR");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
