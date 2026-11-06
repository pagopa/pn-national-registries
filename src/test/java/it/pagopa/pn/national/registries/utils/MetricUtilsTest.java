package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.metrics.DimensionName;
import it.pagopa.pn.national.registries.model.metrics.MetricName;
import it.pagopa.pn.national.registries.model.metrics.MetricUnit;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricUtilsTest {

    @Test
    void generateGeneralMetricWithOnlyMetricNameAndMetricValueSetsAllFieldsCorrectly() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(MetricName.BATCH_SIZE, 42);

        assertEquals("PN-NationalRegistries-Downstream", metric.getNamespace());
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
        assertEquals("PN-NationalRegistries-Downstream", metric.getNamespace());
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

    @Test
    void logCfRequestedMetricDoesNotThrow() {
        assertDoesNotThrow(() -> MetricUtils.logCfRequestedMetric("correlationId", GatewayDownstreamService.INAD, 3));
    }

    @Test
    void logCfWithErrorMetricDoesNotThrowForEveryRecipientType() {
        for (RecipientType recipientType : RecipientType.values()) {
            assertDoesNotThrow(() -> MetricUtils.logCfWithErrorMetric("correlationId", GatewayDownstreamService.INIPEC, recipientType));
        }
    }

    @Test
    void logCfWithAddressMetricAcceptsNullOptionalDimensions() {
        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetric("correlationId", null, null, null));
        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetric("correlationId", GatewayDownstreamService.INAD, RecipientType.PF, DigitalAddressRecipientType.PERSONALE));
    }

    @Test
    void logBatchRequestMetricsDoesNotThrowForSuccessAndFailure() {
        IniPecBatchRequest request = new IniPecBatchRequest();
        IniPecBatchRequest.IniPecCf cf = new IniPecBatchRequest.IniPecCf();
        cf.setCf("12345678901");
        request.setElencoCf(List.of(cf));

        assertDoesNotThrow(() -> MetricUtils.logBatchRequestMetrics("batchId", request, false));
        assertDoesNotThrow(() -> MetricUtils.logBatchRequestMetrics("batchId", request, true));
    }

    @Test
    void logBatchRequestMetricsFailsWhenCfListIsMissing() {
        IniPecBatchRequest request = new IniPecBatchRequest();

        assertThrows(NullPointerException.class, () -> MetricUtils.logBatchRequestMetrics("batchId", request, false));
    }

    @Test
    void logBatchEndingMetricsDoesNotThrowForEveryBatchStatus() {
        BatchPolling polling = new BatchPolling();
        polling.setBatchId("batchId");
        polling.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));

        assertDoesNotThrow(() -> MetricUtils.logBatchEndingMetrics(polling, BatchStatus.ERROR));
        assertDoesNotThrow(() -> MetricUtils.logBatchEndingMetrics(polling, BatchStatus.WORKED));
    }

    @Test
    void logInipecCfRequestedMetricIgnoresNullCount() {
        assertDoesNotThrow(() -> MetricUtils.logInipecCfRequestedMetric("batchId", GatewayDownstreamService.INIPEC, null));
        assertDoesNotThrow(() -> MetricUtils.logInipecCfRequestedMetric("batchId", GatewayDownstreamService.INIPEC, 5));
    }

    @Test
    void logCfWithAddressMetricFromBatchRequestIgnoresNullOrEmptyAddresses() {
        CodeSqsDto empty = new CodeSqsDto();
        empty.setDigitalAddress(List.of());

        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetricFromBatchRequest(null, "correlationId", "RSSMRA80A01H501U", "PF", GatewayDownstreamService.INAD));
        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetricFromBatchRequest(empty, "correlationId", "RSSMRA80A01H501U", "PF", GatewayDownstreamService.INAD));
    }

    @Test
    void logCfWithAddressMetricFromBatchRequestLogsWhenAddressPresent() {
        DigitalAddress address = new DigitalAddress();
        address.setAddress("pec@pec.it");
        address.setRecipient(DigitalAddressRecipientType.IMPRESA.getValue());
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(List.of(address));

        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetricFromBatchRequest(codeSqsDto, "correlationId", "12345678901", null, GatewayDownstreamService.INIPEC));
        assertDoesNotThrow(() -> MetricUtils.logCfWithAddressMetricFromBatchRequest(codeSqsDto, "correlationId", "RSSMRA80A01H501U", "PG", GatewayDownstreamService.INIPEC));
    }
}