package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.national.registries.model.metrics.MetricName;
import it.pagopa.pn.national.registries.model.metrics.MetricUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetricUtilsTest {

    @Test
    void generateGeneralMetricWithOnlyMetricNameAndMetricValueSetsAllFieldsCorrectly() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 42);

        assertEquals("national-registries-inipec", metric.getNamespace());
        assertEquals(1, metric.getMetrics().size());
        assertEquals(MetricName.BATCH_SIZE.getValue(), metric.getMetrics().getFirst().getName());
        assertEquals(42, metric.getMetrics().getFirst().getValue());
        assertNull(metric.getUnit());
    }

    @Test
    void generateGeneralMetricSetsUnitWhenProvided() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_CLOSURE_DURATION, 10, MetricUnit.SECONDS);

        assertEquals(MetricUnit.SECONDS.getValue(), metric.getUnit());
    }
}