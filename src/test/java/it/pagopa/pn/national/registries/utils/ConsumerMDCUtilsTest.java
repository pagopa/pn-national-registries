package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

class ConsumerMDCUtilsTest {
    @Test
    void testAddMessageHeadersToMDC_WithHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_messageId", "msg-123");
        headers.put("X-Amzn-Trace-Id", "trace-456");

        try (MockedStatic<MDCUtils> mdcUtilsMock = Mockito.mockStatic(MDCUtils.class);
             MockedStatic<MDC> mdcMock = Mockito.mockStatic(MDC.class)) {

            ConsumerMDCUtils.addMessageHeadersToMDC(headers);

            mdcUtilsMock.verify(MDCUtils::clearMDCKeys);
            mdcMock.verify(() -> MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "trace-456"));
            mdcMock.verify(() -> MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, "msg-123"));
        }
    }

    @Test
    void testAddMessageHeadersToMDC_MissingTraceId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_messageId", "msg-789");

        try (MockedStatic<MDCUtils> mdcUtilsMock = Mockito.mockStatic(MDCUtils.class);
             MockedStatic<MDC> mdcMock = Mockito.mockStatic(MDC.class)) {

            ConsumerMDCUtils.addMessageHeadersToMDC(headers);

            mdcUtilsMock.verify(MDCUtils::clearMDCKeys);
            mdcMock.verify(() -> MDC.put(Mockito.eq(MDCUtils.MDC_TRACE_ID_KEY), Mockito.startsWith("traceId:")));
            mdcMock.verify(() -> MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, "msg-789"));
        }
    }

    @Test
    void testAddMessageHeadersToMDC_MissingMessageId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Amzn-Trace-Id", "trace-999");

        try (MockedStatic<MDCUtils> mdcUtilsMock = Mockito.mockStatic(MDCUtils.class);
             MockedStatic<MDC> mdcMock = Mockito.mockStatic(MDC.class)) {

            ConsumerMDCUtils.addMessageHeadersToMDC(headers);

            mdcUtilsMock.verify(MDCUtils::clearMDCKeys);
            mdcMock.verify(() -> MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "trace-999"));
            mdcMock.verify(() -> MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, null));
        }
    }

}