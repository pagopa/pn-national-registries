package it.pagopa.pn.national.registries.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_INVALIDREGISTRYFORPOLLING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingInputsHandlerTest {

    @Mock
    private InipecPollingService inipecPollingService;
    @Mock
    private InadPollingService inadPollingService;

    private PollingInputsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PollingInputsHandler(inipecPollingService, inadPollingService);
    }

    @Test
    void inipecRegistryIsRoutedToInipecPollingService() {
        PnPollingEvent.Payload payload = payload(GatewayDownstreamService.INIPEC);
        when(inipecPollingService.handleMessage(payload)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.pnNationalRegistriesPollingInputsConsumer(message(payload)));

        verify(inipecPollingService).handleMessage(payload);
        verifyNoInteractions(inadPollingService);
    }

    @Test
    void inadRegistryIsRoutedToInadPollingService() {
        PnPollingEvent.Payload payload = payload(GatewayDownstreamService.INAD);
        when(inadPollingService.handleMessage(payload)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.pnNationalRegistriesPollingInputsConsumer(message(payload)));

        verify(inadPollingService).handleMessage(payload);
        verifyNoInteractions(inipecPollingService);
    }

    @ParameterizedTest
    @EnumSource(value = GatewayDownstreamService.class, names = {"ANPR", "REGISTRO_IMPRESE", "IPA"})
    void unsupportedRegistryFailsWithInvalidRegistryError(GatewayDownstreamService registry) {
        Message<PnPollingEvent.Payload> message = message(payload(registry));

        PnInternalException exception = assertThrows(PnInternalException.class,
                () -> handler.pnNationalRegistriesPollingInputsConsumer(message));

        assertEquals(ERROR_CODE_NATIONAL_REGISTRIES_INVALIDREGISTRYFORPOLLING, exception.getProblem().getErrors().get(0).getCode());
        verifyNoInteractions(inipecPollingService, inadPollingService);
    }

    @Test
    void nullRegistryFails() {
        Message<PnPollingEvent.Payload> message = message(payload(null));

        assertThrows(NullPointerException.class, () -> handler.pnNationalRegistriesPollingInputsConsumer(message));

        verifyNoInteractions(inipecPollingService, inadPollingService);
    }

    @Test
    void inipecServiceErrorIsPropagated() {
        PnPollingEvent.Payload payload = payload(GatewayDownstreamService.INIPEC);
        when(inipecPollingService.handleMessage(payload)).thenReturn(Mono.error(new IllegalStateException("polling failed")));
        Message<PnPollingEvent.Payload> message = message(payload);

        assertThrows(IllegalStateException.class, () -> handler.pnNationalRegistriesPollingInputsConsumer(message));
    }

    @Test
    void inadServiceErrorIsPropagated() {
        PnPollingEvent.Payload payload = payload(GatewayDownstreamService.INAD);
        when(inadPollingService.handleMessage(payload)).thenReturn(Mono.error(new IllegalStateException("polling failed")));
        Message<PnPollingEvent.Payload> message = message(payload);

        assertThrows(IllegalStateException.class, () -> handler.pnNationalRegistriesPollingInputsConsumer(message));
    }


    private static PnPollingEvent.Payload payload(GatewayDownstreamService registry) {
        return PnPollingEvent.Payload.builder()
                .registry(registry)
                .batchId("batch-id")
                .pollingId("polling-id")
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private static Message<PnPollingEvent.Payload> message(PnPollingEvent.Payload payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader("aws_messageId", "message-id")
                .setHeader("X-Amzn-Trace-Id", "Root=1-trace")
                .build();
    }
}

