package it.pagopa.pn.national.registries.utils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class CheckExceptionUtilsTest {
    /**
     * Method under test: {@link CheckExceptionUtils#logOnWarningOrError(Throwable, Logger, String)}
     */
    @Test
    void testLogOnWarningOrError1() {
        Throwable throwable = new Throwable();
        Logger logger = mock(Logger.class);
        doNothing().when(logger).error(any());
        CheckExceptionUtils.logOnWarningOrError(throwable, logger, "Error message");
        verify(logger).error(any());
    }

    /**
     * Method under test: {@link CheckExceptionUtils#logOnWarningOrError(Throwable, Logger, String)}
     */
    @Test
    void testLogOnWarningOrError2() {
        Logger logger = mock(Logger.class);
        doNothing().when(logger).warn(any());
        WebClientResponseException webClientResponseException = new WebClientResponseException(400, "BAD_REQUEST", null, null, null);
        CheckExceptionUtils.logOnWarningOrError(webClientResponseException, logger, "Error message.");
        verify(logger).warn(any());
    }
}

