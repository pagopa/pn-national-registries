package it.pagopa.pn.national.registries.model.metrics;

import lombok.Getter;

@Getter
public enum MetricUnit {
    SECONDS("Seconds");

    private final String value;

    MetricUnit(String value) {
        this.value = value;
    }

}
