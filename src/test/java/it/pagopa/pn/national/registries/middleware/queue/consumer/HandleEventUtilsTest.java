package it.pagopa.pn.national.registries.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

class HandleEventUtilsTest {

    @Test
    void mapStandardEventHeader() {

        StandardEventHeader actual = HandleEventUtils.mapStandardEventHeader(buildMessageHeaders());

        Assertions.assertEquals(buildStandardEventHeader(), actual);
    }

    @Test
    void mapStandardEventHeaderException() {

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            HandleEventUtils.mapStandardEventHeader(null);
        });

        String expectErrorMsg = "PN_NATIONAL_REGISTRIES_HANDLEEVENTFAILED";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }


    @Test
    void mapStandardEventHeaderWithoutValuesKeepsNullFields() {
        StandardEventHeader actual = HandleEventUtils.mapStandardEventHeader(new MessageHeaders(new HashMap<>()));

        Assertions.assertNull(actual.getEventId());
        Assertions.assertNull(actual.getIun());
        Assertions.assertNull(actual.getEventType());
        Assertions.assertNull(actual.getCreatedAt());
        Assertions.assertNull(actual.getPublisher());
    }

    @Test
    void handleExceptionWithHeaders() {
        Assertions.assertDoesNotThrow(() ->
                HandleEventUtils.handleException(buildMessageHeaders(), new RuntimeException("error", new IllegalStateException("cause"))));
    }

    @Test
    void handleExceptionWithHeadersAndExceptionWithoutCause() {
        Assertions.assertDoesNotThrow(() ->
                HandleEventUtils.handleException(buildMessageHeaders(), new RuntimeException("error")));
    }

    @Test
    void handleExceptionWithNullHeaders() {
        Assertions.assertDoesNotThrow(() ->
                HandleEventUtils.handleException(null, new RuntimeException("error")));
    }

    private MessageHeaders buildMessageHeaders() {
        Map<String, Object> map = new HashMap<>();
        map.put(PN_EVENT_HEADER_EVENT_ID, "001");
        map.put(PN_EVENT_HEADER_IUN, "002");
        map.put(PN_EVENT_HEADER_EVENT_TYPE, "003");
        map.put(PN_EVENT_HEADER_CREATED_AT, "2021-09-16T15:23:00.00Z");
        map.put(PN_EVENT_HEADER_PUBLISHER, "005");
        return new MessageHeaders(map);
    }

    private StandardEventHeader buildStandardEventHeader() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        return StandardEventHeader.builder()
                .eventId("001")
                .iun("002")
                .eventType("003")
                .createdAt(instant)
                .publisher("005")
                .build();
    }
}