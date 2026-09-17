package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.metrics.DimensionName;
import it.pagopa.pn.national.registries.model.metrics.MetricName;
import it.pagopa.pn.national.registries.model.metrics.MetricUnit;
import it.pagopa.pn.national.registries.model.metrics.StatusDimension;
import lombok.CustomLog;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;

@CustomLog
public class MetricUtils {
    private static final String METRIC_NAMESPACE = "PN-NationalRegistries-Downstream";


    private MetricUtils() {
    }

    public static Dimension generateDimension(DimensionName name, String value) {
        Dimension dimension = new Dimension();
        dimension.setName(name.getValue());
        dimension.setValue(value);
        return dimension;
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue) {
        return generateGeneralMetric(metricName, metricValue, null, null);
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, List<Dimension> dimensions) {
        return generateGeneralMetric(metricName, metricValue, dimensions, null);
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, List<Dimension> dimensions, MetricUnit unit) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setDimensions(dimensions);
        generalMetric.setTimestamp(Instant.now().toEpochMilli());

        if (unit != null) {
            generalMetric.setUnit(unit.getValue());
        }

        return generalMetric;
    }

    public static void logCfRequestedMetric(String correlationId, GatewayDownstreamService registry, Integer count) {
        String logMessage = "Logging CF_REQUESTED metrics for correlationId: " + correlationId + " - called EService:  " + registry;
        List<GeneralMetric> requestMetrics = new ArrayList<>();
        requestMetrics.add(
                generateGeneralMetric(
                        MetricName.CF_REQUESTED,
                        count,
                        List.of(generateDimension(DimensionName.REGISTRY, registry.name()))
                )
        );

        log.logMetric(requestMetrics, logMessage);
    }

    public static void logCfWithAddressMetric(String correlationId, GatewayDownstreamService registry, RecipientType recipientType, DigitalAddressRecipientType digitalAddressRecipientType) {
        String logMessage = "Logging CF_WITH_ADDRESS metrics for correlationId: " + correlationId + " - called EService:  " + registry + " for recipientType: " + recipientType;
        List<GeneralMetric> requestMetrics = List.of(
                generateGeneralMetric(
                        MetricName.CF_WITH_ADDRESS,
                        1,
                        generateDimensionForCfWithAddress(registry, recipientType, digitalAddressRecipientType)));

        log.logMetric(requestMetrics, logMessage);
    }

    private static List<Dimension> generateDimensionForCfWithAddress(GatewayDownstreamService registry, RecipientType recipientType, DigitalAddressRecipientType digitalAddressRecipientType) {
        List<Dimension> dimensions = new ArrayList<>();
        if(Objects.nonNull(registry)) {
            dimensions.add(generateDimension(DimensionName.REGISTRY, registry.name()));
        }
        if(Objects.nonNull(recipientType)) {
            dimensions.add(generateDimension(DimensionName.NOTIFICATION_SCOPE, recipientType.name()));
        }
        if (Objects.nonNull(digitalAddressRecipientType)) {
            dimensions.add(generateDimension(DimensionName.DIGITAL_ADDRESS_SCOPE, digitalAddressRecipientType.getValue()));
        }
        return dimensions;
    }

    public static void logBatchRequestMetrics(String batchId, IniPecBatchRequest iniPecBatchRequest, boolean isError) {
        StatusDimension status = isError ? StatusDimension.FAILURE : StatusDimension.OK;
        String logMessage = "IniPEC - Logging batch request metrics for batchId: " + batchId + " - called EService and batch size is: " + iniPecBatchRequest.getElencoCf().size() + " with status: " + status;
        List<GeneralMetric> requestMetrics = List.of(
                MetricUtils.generateGeneralMetric(
                        MetricName.BATCH_REQUEST_CREATION,
                        1,
                        List.of(MetricUtils.generateDimension(DimensionName.STATUS, status.name()))
                ),
                MetricUtils.generateGeneralMetric(
                        MetricName.BATCH_SIZE,
                        iniPecBatchRequest.getElencoCf().size(),
                        List.of(MetricUtils.generateDimension(DimensionName.STATUS, status.name()))
                )
        );

        log.logMetric(requestMetrics, logMessage);
    }

    public static void logBatchEndingMetrics(BatchPolling polling, BatchStatus batchStatus) {
        StatusDimension status = batchStatus == BatchStatus.ERROR ? StatusDimension.FAILURE : StatusDimension.OK;
        long batchClosureDurationMillis = Instant.now().toEpochMilli() - polling.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        int batchClosureDurationSeconds = (int) (batchClosureDurationMillis / 1000);

        List<GeneralMetric> batchEndingMetrics = List.of(
                MetricUtils.generateGeneralMetric(
                        MetricName.BATCH,
                        1,
                        List.of(MetricUtils.generateDimension(DimensionName.STATUS, status.name()))
                ),
                MetricUtils.generateGeneralMetric(
                        MetricName.BATCH_CLOSURE_DURATION,
                        batchClosureDurationSeconds,
                        List.of(MetricUtils.generateDimension(DimensionName.STATUS, status.name())),
                        MetricUnit.SECONDS
                )
        );

        log.logMetric(batchEndingMetrics, "IniPEC - Logging batch ending metrics for batchId: " + polling.getBatchId() + " with status: " + status);
    }

    public static void logInipecCfRequestedMetric(String batchId, GatewayDownstreamService registry, Integer count) {
        String logMessage = "Logging CF_REQUESTED metrics for batchId: " + batchId + " - called EService:  " + registry;
        if (Objects.nonNull(count)) {
            List<GeneralMetric> requestMetrics = new ArrayList<>();
            requestMetrics.add(
                    generateGeneralMetric(
                            MetricName.CF_REQUESTED,
                            count,
                            List.of(generateDimension(DimensionName.REGISTRY, registry.name()))
                    )
            );
            log.logMetric(requestMetrics, logMessage);
        }
    }

    public static void logCfWithAddressMetricFromBatchRequest(CodeSqsDto codeSqsDto, BatchRequest batchRequest, GatewayDownstreamService registry) {
        if(Objects.nonNull(batchRequest) && Objects.nonNull(codeSqsDto) && !CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())){
            RecipientType recipientType = retrieveRecipientType(batchRequest.getCf(), batchRequest.getRecipientType());
            DigitalAddressRecipientType digitalAddressRecipientType = DigitalAddressRecipientType.fromValue(codeSqsDto.getDigitalAddress().getFirst().getRecipient());
            logCfWithAddressMetric(batchRequest.getCorrelationId(), registry, recipientType, digitalAddressRecipientType);
        }
    }
}
