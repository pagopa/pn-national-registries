package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestIdUtilsTest {

    private ServerWebExchange exchange;
    private HttpHeaders responseHeaders;

    @BeforeEach
    void setUp() {
        exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        responseHeaders = new HttpHeaders();
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldUseCorrelationIdWhenPresent() {
        String correlationId = "correlation-id-123";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "Root=1-6ae31dcc-7de57c8214838adc1da3772d");

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(correlationId, exchange);
        assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(correlationId, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldUseNormalizedTraceIdWhenCorrelationIdIsNull() {
        String traceId = "Root=1-6ae31dcc-7de57c8214838adc1da3772d";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        String expected = "1-6ae31dcc-7de57c8214838adc1da3772d";
        assertEquals(expected, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(expected, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldUseNormalizedTraceIdWhenCorrelationIdIsBlank() {
        String traceId = "Root=1-6ae31dcc-7de57c8214838adc1da3772d";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader("   ", exchange);

        String expected = "1-6ae31dcc-7de57c8214838adc1da3772d";
        assertEquals(expected, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(expected, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldKeepTraceIdUnchangedWhenRootPrefixIsMissing() {
        String traceId = "1-6ae31dcc-7de57c8214838adc1da3772d";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(traceId, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldExtractRootFromFullAmznTraceIdHeader() {
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY,
                "Self=1-6aa7e644-303ca39c5d0966104baa3386;Root=1-213d2cb3-67951230778a69cd871e70db;Parent=3eebdf2ccf7aa640;Sampled=0");

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        String expected = "1-213d2cb3-67951230778a69cd871e70db";
        assertEquals(expected, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(expected, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldExtractRootWhenFollowedByOtherSegments() {
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "Root=1-6ae31dcc-7de57c8214838adc1da3772d;Parent=3eebdf2ccf7aa640;Sampled=1");

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        String expected = "1-6ae31dcc-7de57c8214838adc1da3772d";
        assertEquals(expected, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(expected, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldKeepGeneratedTraceIdWhenRootSegmentIsMissing() {
        String traceId = "trace_id:2b6c1a3e-5f4d-4e8a-9c7b-1d2e3f4a5b6c";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(traceId, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldKeepRawTraceIdWhenRootSegmentIsEmpty() {
        String traceId = "Root=;Parent=3eebdf2ccf7aa640;Sampled=0";
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertEquals(traceId, responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldNotSetRequestIdWhenCorrelationIdAndTraceIdAreNull() {
        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertNull(responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }

    @Test
    void shouldNotSetRequestIdWhenCorrelationIdAndTraceIdAreBlank() {
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "   ");

        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader("   ", exchange);

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertNull(responseHeaders.getFirst(RequestIdUtils.X_REQUEST_ID));
    }
}