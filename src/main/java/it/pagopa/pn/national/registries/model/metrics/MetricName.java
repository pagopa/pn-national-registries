package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricName {
    INIPEC_REQUEST_INVOCATIONS("InipecRequestInvocations"),
    INIPEC_REQUEST_BATCH_CLOSURE_DURATION("InipecRequestBatchClosureDuration"),
    INIPEC_REQUEST_BATCH_SIZE("InipecRequestBatchSize"),
    INIPEC_REQUEST_ERROR("InipecRequestError"),
    INIPEC_POLLING_ERROR("InipecPollingError");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

}
