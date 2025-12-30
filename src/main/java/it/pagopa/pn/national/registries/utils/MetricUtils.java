package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import it.pagopa.pn.national.registries.model.metrics.MetricName;
import it.pagopa.pn.national.registries.model.metrics.MetricUnit;

import java.time.Instant;
import java.util.List;

public class MetricUtils {
    private final static String METRIC_NAMESPACE = "national-registries-inipec";

    private MetricUtils() {
    }

    public static List<GeneralMetric> generateGeneralMetrics(MetricName metricName, int metricValue, MetricUnit unit) {
        return List.of(generateGeneralMetric(metricName, metricValue, unit));
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, MetricUnit unit) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setTimestamp(Instant.now().toEpochMilli());

        if (unit != null) {
            generalMetric.setUnit(unit.getValue());
        }

        return generalMetric;
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue) {
        return generateGeneralMetric(metricName, metricValue, null);
    }
}
