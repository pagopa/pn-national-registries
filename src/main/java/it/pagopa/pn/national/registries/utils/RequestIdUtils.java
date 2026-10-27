package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public class RequestIdUtils {
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";
    public static final String X_REQUEST_ID = "X-Request-ID";

    private RequestIdUtils() {
    }

    public static String putRequestIdToMDC() {
        return putRequestIdToMDC(null);
    }

    public static String putRequestIdToMDC(String correlationId) {
        String requestId = StringUtils.hasText(correlationId)
                ? correlationId
                : MDC.get(AWS_XRAY_TRACE_ID);

        if (StringUtils.hasText(requestId)) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
        }

        return requestId;
    }

}
