package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;

public class RequestIdUtils {
    public static final String X_REQUEST_ID = "X-Request-ID";

    private RequestIdUtils() {
    }

    public static void putRequestIdToMDCAndExchangeResponseHeader(String correlationId, ServerWebExchange exchange) {
        String requestId = StringUtils.isNotBlank(correlationId) ? correlationId :  normalizeTraceId(MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        if (StringUtils.isNotBlank(requestId)) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
            exchange.getResponse().getHeaders().set(X_REQUEST_ID, requestId);
        }
    }

    private static String normalizeTraceId(String traceId) {
        if (StringUtils.isNotBlank(traceId)) {
            return StringUtils.removeStart(traceId, "Root=");
        }
        return traceId;
    }

}
