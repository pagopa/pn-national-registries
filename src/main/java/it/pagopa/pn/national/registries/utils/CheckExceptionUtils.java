package it.pagopa.pn.national.registries.utils;

import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class CheckExceptionUtils {

    private CheckExceptionUtils() {}
    private static boolean checkExceptionStatusForLogLevel(Throwable throwable) {
        return throwable instanceof WebClientResponseException webClientResponseException
                && !webClientResponseException.getStatusCode().is5xxServerError();
    }

    public static void logOnWarningOrError(Throwable throwable, Logger log, String message) {
        if(checkExceptionStatusForLogLevel(throwable))
            log.warn(message);
        else
            log.error(message);
    }
}
