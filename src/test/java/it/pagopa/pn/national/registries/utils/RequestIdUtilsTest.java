package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestIdUtilsTest {

    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldPutCorrelationIdIntoMdcWhenCorrelationIdHasText() {
        String correlationId = "corr-id-123";

        RequestIdUtils.putRequestIdToMDC(correlationId);

        assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldUseAwsXrayTraceIdWhenCorrelationIdIsNull() {
        String awsTraceId = "aws-trace-123";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDC(null);

        assertEquals(awsTraceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldUseAwsXrayTraceIdWhenCorrelationIdIsBlank() {
        String awsTraceId = "aws-trace-456";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDC("   ");

        assertEquals(awsTraceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldNotPutAnythingIntoMdcWhenCorrelationIdAndAwsTraceIdAreMissing() {
        RequestIdUtils.putRequestIdToMDC(null);

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldNotPutAnythingIntoMdcWhenCorrelationIdIsBlankAndAwsTraceIdIsBlank() {
        MDC.put(AWS_XRAY_TRACE_ID, "   ");

        RequestIdUtils.putRequestIdToMDC("   ");

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldPreferCorrelationIdOverAwsXrayTraceId() {
        String correlationId = "corr-id-preferred";
        String awsTraceId = "aws-trace-id";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDC(correlationId);

        assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }
}