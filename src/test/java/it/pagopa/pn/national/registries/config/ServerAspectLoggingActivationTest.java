package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerAspectLoggingActivationTest {

    @Test
        void testServerAspectLoggingActivation() {
            ServerAspectLoggingActivation serverAspectLoggingActivation = new ServerAspectLoggingActivation();
            assertNotNull(serverAspectLoggingActivation);
        }
}