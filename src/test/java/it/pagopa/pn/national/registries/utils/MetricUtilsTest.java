package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.national.registries.model.metrics.DimensionName;
import it.pagopa.pn.national.registries.model.metrics.MetricName;
import it.pagopa.pn.national.registries.model.metrics.MetricUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricName.BATCH_CLOSURE_DURATION,
                10,
                List.of(MetricUtils.generateDimension(DimensionName.STATUS, "OK")),
                MetricUnit.SECONDS
        );

        assertEquals(MetricUnit.SECONDS.getValue(), metric.getUnit());
    }

    @Test
    void generateDimensionMapsNameAndValue() {
        Dimension dimension = MetricUtils.generateDimension(DimensionName.STATUS, "OK");

        assertEquals(DimensionName.STATUS.getValue(), dimension.getName());
        assertEquals("OK", dimension.getValue());
    }

    @Test
    void generateGeneralMetricWithDimensionsSetsTimestampAndDimensions() {
        Dimension dimension = MetricUtils.generateDimension(DimensionName.STATUS, "OK");
        List<Dimension> dimensions = List.of(dimension);
        long before = System.currentTimeMillis();

        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 3, dimensions);

        long after = System.currentTimeMillis();
        assertEquals("national-registries-inipec", metric.getNamespace());
        assertEquals(dimensions, metric.getDimensions());
        assertTrue(metric.getTimestamp() >= before && metric.getTimestamp() <= after);
    }

    @Test
    void generateGeneralMetricWithNullDimensionsKeepsDimensionsNull() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 3, null);

        assertNull(metric.getDimensions());
    }

    @Test
    void generateGeneralMetricWithUnitAndDimensionsSetsUnitAndMetrics() {
        Dimension dimension = MetricUtils.generateDimension(DimensionName.STATUS, "OK");
        List<Dimension> dimensions = List.of(dimension);

        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_CLOSURE_DURATION, 10, dimensions, MetricUnit.SECONDS);

        assertEquals(MetricUnit.SECONDS.getValue(), metric.getUnit());
        assertNotNull(metric.getMetrics());
        assertEquals(1, metric.getMetrics().size());
        assertEquals(MetricName.BATCH_CLOSURE_DURATION.getValue(), metric.getMetrics().getFirst().getName());
        assertEquals(10, metric.getMetrics().getFirst().getValue());
    }
}