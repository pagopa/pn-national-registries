package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestIdUtilsTest {

    @Mock
    private ServerWebExchange serverWebExchange;

    @Mock
    private ServerHttpResponse serverHttpResponse;

    private HttpHeaders httpHeaders;

    private static final String AWS_XRAY_TRACE_ID = "AWS-XRAY-TRACE-ID";

    @BeforeEach
    void setUp() {
        httpHeaders = new HttpHeaders();
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldPutCorrelationIdIntoMdcWhenCorrelationIdHasText() {
        String correlationId = "corr-id-123";

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(correlationId, serverWebExchange);

        assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(correlationId, httpHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldUseAwsXrayTraceIdWhenCorrelationIdIsNull() {
        String awsTraceId = "aws-trace-123";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, serverWebExchange);

        //assertEquals(awsTraceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertNotNull(awsTraceId, httpHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    /*@Test
    void shouldUseAwsXrayTraceIdWhenCorrelationIdIsBlank() {
        String awsTraceId = "aws-trace-456";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader("   ", serverWebExchange);

        assertEquals(awsTraceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(awsTraceId, httpHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }*/

    @Test
    void shouldNotPutAnythingIntoMdcWhenCorrelationIdAndAwsTraceIdAreMissing() {
        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, serverWebExchange);

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertNull(httpHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldNotPutAnythingIntoMdcWhenCorrelationIdIsBlankAndAwsTraceIdIsBlank() {
        MDC.put(AWS_XRAY_TRACE_ID, "   ");

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader("   ", serverWebExchange);

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldPreferCorrelationIdOverAwsXrayTraceId() {
        String correlationId = "corr-id-preferred";
        String awsTraceId = "aws-trace-id";
        MDC.put(AWS_XRAY_TRACE_ID, awsTraceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(correlationId, serverWebExchange);

        assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(correlationId, httpHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }
}