package it.pagopa.pn.national.registries.middleware.queue.consumer;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayInputsHandlerTest {

    @Mock
    private GatewayService gatewayService;

    private GatewayInputsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GatewayInputsHandler(gatewayService);
    }

    @Test
    void messageIsDelegatedToGatewayService() {
        PnAddressGatewayEvent.Payload payload = payload();
        when(gatewayService.handleMessage(payload)).thenReturn(Mono.just(new AddressOKDto()));

        assertDoesNotThrow(() -> handler.pnNationalRegistriesGatewayRequestConsumer(message(payload)));

        verify(gatewayService).handleMessage(payload);
    }

    @Test
    void emptyResultIsAccepted() {
        PnAddressGatewayEvent.Payload payload = payload();
        when(gatewayService.handleMessage(payload)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.pnNationalRegistriesGatewayRequestConsumer(message(payload)));
    }

    @Test
    void serviceErrorIsPropagated() {
        PnAddressGatewayEvent.Payload payload = payload();
        when(gatewayService.handleMessage(payload)).thenReturn(Mono.error(new IllegalStateException("gateway failed")));
        Message<PnAddressGatewayEvent.Payload> message = message(payload);

        assertThrows(IllegalStateException.class, () -> handler.pnNationalRegistriesGatewayRequestConsumer(message));
    }


    private static PnAddressGatewayEvent.Payload payload() {
        return PnAddressGatewayEvent.Payload.builder()
                .correlationId("correlation-id")
                .taxId("RSSMRA80A01H501U")
                .domicileType("PHYSICAL")
                .recipientType("PF")
                .pnNationalRegistriesCxId("cx-id")
                .build();
    }

    private static Message<PnAddressGatewayEvent.Payload> message(PnAddressGatewayEvent.Payload payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader("aws_messageId", "message-id")
                .setHeader("X-Amzn-Trace-Id", "Root=1-trace")
                .build();
    }
}

