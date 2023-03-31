package it.pagopa.pn.national.registries.utils;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class CheckExceptionUtils {

    private CheckExceptionUtils() {

    }

    public static boolean isForLogLevelWarn(Throwable throwable) {
        return throwable instanceof WebClientResponseException exception
                && !exception.getStatusCode().is5xxServerError();
    }
}
