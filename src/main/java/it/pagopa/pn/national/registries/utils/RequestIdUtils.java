package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestIdUtils {
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    public static void putRequestIdToMDC(String correlationId) {
        String requestId = hasText(correlationId)
                ? correlationId
                : MDC.get(AWS_XRAY_TRACE_ID);

        if (hasText(requestId)) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
