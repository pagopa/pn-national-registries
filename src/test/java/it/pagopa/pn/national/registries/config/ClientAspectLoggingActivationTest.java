package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientAspectLoggingActivationTest {
    @Test
    void testClientAspectLoggingActivation() {
        ClientAspectLoggingActivation serverAspectLoggingActivation = new ClientAspectLoggingActivation();
        assertNotNull(serverAspectLoggingActivation);
    }
}