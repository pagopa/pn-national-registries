package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringApplicationUtilsTest {

    @Test
    void testBuildSpringApplicationWithListener() {
        SpringApplication app = SpringApplicationUtils.buildSpringApplicationWithListener();
        assertNotNull(app, "SpringApplication should not be null");

        boolean listenerFound = app.getListeners().stream()
                .anyMatch(listener -> listener instanceof TaskIdApplicationListener);
        assertTrue(listenerFound, "TaskIdApplicationListener should be added to the SpringApplication");
    }
}