package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

public class RequestIdUtils {
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_AMZ_TRACE_ID = "X-Amzn-Trace-Id";

    private RequestIdUtils() {
    }

    public static void putRequestIdToMDCAndExchangeResponseHeader(String correlationId, ServerWebExchange exchange) {
        String requestId = putRequestIdToMDC(correlationId, exchange);
        exchange.getResponse().getHeaders().set(X_REQUEST_ID, requestId);
    }

    private static String putRequestIdToMDC(String correlationId, ServerWebExchange exchange) {
        String requestId = StringUtils.hasText(correlationId) ? correlationId : exchange.getRequest().getHeaders().getFirst(X_AMZ_TRACE_ID);

        if (StringUtils.hasText(requestId)) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
        }

        return requestId;
    }

}
