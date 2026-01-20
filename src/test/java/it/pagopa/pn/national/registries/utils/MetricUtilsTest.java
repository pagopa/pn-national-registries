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
    void generateDimensionReturnsCorrectDimension() {
        Dimension dimension = MetricUtils.generateDimension(DimensionName.BATCH_TYPE, "value1");
        assertEquals(DimensionName.BATCH_TYPE.getValue(), dimension.getName());
        assertEquals("value1", dimension.getValue());
    }

    @Test
    void generateGeneralMetricSetsAllFieldsCorrectlyWithoutUnit() {
        List<Dimension> dimensions = List.of(MetricUtils.generateDimension(DimensionName.BATCH_TYPE, "test"));
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 42, dimensions);

        assertEquals("national-registries-inipec", metric.getNamespace());
        assertEquals(1, metric.getMetrics().size());
        assertEquals(MetricName.BATCH_SIZE.getValue(), metric.getMetrics().getFirst().getName());
        assertEquals(42, metric.getMetrics().getFirst().getValue());
        assertEquals(dimensions, metric.getDimensions());
        assertNull(metric.getUnit());
    }

    @Test
    void generateGeneralMetricSetsUnitWhenProvided() {
        List<Dimension> dimensions = List.of(MetricUtils.generateDimension(DimensionName.BATCH_TYPE, "test"));
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 10, dimensions, MetricUnit.SECONDS);

        assertEquals(MetricUnit.SECONDS.getValue(), metric.getUnit());
    }

    @Test
    void generateGeneralMetricsReturnsListWithSingleGeneralMetric() {
        List<Dimension> dimensions = List.of(MetricUtils.generateDimension(DimensionName.BATCH_TYPE, "test"));
        List<GeneralMetric> metrics = MetricUtils.generateGeneralMetrics(MetricName.BATCH_SIZE, 5, dimensions);

        assertEquals(1, metrics.size());
        assertEquals(MetricName.BATCH_SIZE.getValue(), metrics.getFirst().getMetrics().getFirst().getName());
        assertNull(metrics.getFirst().getUnit());
    }

    @Test
    void generateGeneralMetricsWithUnitReturnsListWithUnitSet() {
        List<Dimension> dimensions = List.of(MetricUtils.generateDimension(DimensionName.BATCH_TYPE, "test"));
        List<GeneralMetric> metrics = MetricUtils.generateGeneralMetrics(MetricName.BATCH_SIZE, 5, dimensions, MetricUnit.SECONDS);

        assertEquals(1, metrics.size());
        assertEquals(MetricUnit.SECONDS.getValue(), metrics.getFirst().getUnit());
    }

    @Test
    void generateGeneralMetricWithEmptyDimensionsDoesNotFail() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 1, List.of());
        assertNotNull(metric);
        assertTrue(metric.getDimensions().isEmpty());
    }
}