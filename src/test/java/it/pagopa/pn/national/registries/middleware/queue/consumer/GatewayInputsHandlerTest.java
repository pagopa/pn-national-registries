package it.pagopa.pn.national.registries.middleware.queue.consumer;


import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@FunctionalSpringBootTest
class GatewayInputsHandlerTest {
    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private GatewayService gatewayService;

    @Test
    void consumeMessageOK() {
        Consumer<Message<PnAddressGatewayEvent.Payload>> pnNationalRegistriesEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnNationalRegistriesGatewayRequestConsumer");

        PnAddressGatewayEvent event = PnAddressGatewayEvent.builder()
                .payload(PnAddressGatewayEvent.Payload.builder()
                        .correlationId("ADDRESS_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1")
                        .recipientType("PG")
                        .domicileType("DIGITAL")
                        .pnNationalRegistriesCxId("cxId")
                        .build())
                .build();

        when(gatewayService.handleMessage(any())).thenReturn(Mono.just(new AddressOKDto()));
        Message<PnAddressGatewayEvent.Payload> message = MessageBuilder.withPayload(event.getPayload()).build();
        pnNationalRegistriesEventInboundConsumer.accept(message);
    }

}