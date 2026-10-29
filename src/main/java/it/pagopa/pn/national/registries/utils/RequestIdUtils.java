package it.pagopa.pn.national.registries.utils;

import com.amazonaws.xray.AWSXRay;
import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

public class RequestIdUtils {
    public static final String X_REQUEST_ID = "X-Request-ID";

    private RequestIdUtils() {
    }

    public static void putRequestIdToMDCAndExchangeResponseHeader(String correlationId, ServerWebExchange exchange) {
        String requestId = putRequestIdToMDC(correlationId);
        exchange.getResponse().getHeaders().set(X_REQUEST_ID, requestId);
    }

    private static String putRequestIdToMDC(String correlationId) {
        String xrayTraceId = AWSXRay.currentFormattedId();
        String requestId = StringUtils.hasText(correlationId) ? correlationId : xrayTraceId;

        if (StringUtils.hasText(requestId)) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
        }

        return requestId;
    }

}
