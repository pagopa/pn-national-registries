package it.pagopa.pn.national.registries.middleware.queue.consumer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventHandlerTest {

    @Test
    void handlerMapIsNullByDefault() {
        assertNull(new EventHandler().getHandler());
    }

    @Test
    void handlerMapIsStored() {
        EventHandler eventHandler = new EventHandler();
        Map<String, String> handlers = Map.of("NR_GATEWAY_INPUT", "pnNationalRegistriesGatewayRequestConsumer");

        eventHandler.setHandler(handlers);

        assertEquals(handlers, eventHandler.getHandler());
        assertEquals("pnNationalRegistriesGatewayRequestConsumer", eventHandler.getHandler().get("NR_GATEWAY_INPUT"));
    }

    @Test
    void equalsAndHashCodeDependOnHandlerMap() {
        EventHandler first = new EventHandler();
        EventHandler second = new EventHandler();
        first.setHandler(Map.of("k", "v"));
        second.setHandler(Map.of("k", "v"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        second.setHandler(Map.of("k", "other"));
        assertNotEquals(first, second);
    }
}

