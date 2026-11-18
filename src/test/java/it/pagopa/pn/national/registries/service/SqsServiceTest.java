package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayResponseEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.GatewayResponseQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqsServiceTest {

    private GatewayResponseQueueMomProducer outputQueueMomProducer;
    private InternalQueueMomProducer internalQueueMomProducer;
    private InternalDlqQueueMomProducer internalDlqQueueMomProducer;

    private SqsService sqsService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        outputQueueMomProducer = mock(GatewayResponseQueueMomProducer.class);
        internalQueueMomProducer = mock(InternalQueueMomProducer.class);
        internalDlqQueueMomProducer = mock(InternalDlqQueueMomProducer.class);
        objectMapper = mock(ObjectMapper.class);

        sqsService = new SqsService(
                outputQueueMomProducer,
                internalQueueMomProducer,
                internalDlqQueueMomProducer,
                objectMapper
        );
    }

    @Test
    void testPushToOutputQueue() {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        GatewayResponse gatewayResponse = new GatewayResponse(codeSqsDto, "client-id");
        StepVerifier.create(
                        sqsService.pushToOutputQueue(gatewayResponse)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayResponseEvent> captor =
                ArgumentCaptor.forClass(GatewayResponseEvent.class);

        verify(outputQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayResponseEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals("NR_GATEWAY_RESPONSE", event.getHeader().getEventType());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(gatewayResponse, event.getPayload());

        verifyNoInteractions(internalQueueMomProducer);
        verifyNoInteractions(internalDlqQueueMomProducer);
    }

    @Test
    void testPushToInputQueue() {
        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder()
                .correlationId("correlationId")
                .build();

        String clientId = "clientId";

        StepVerifier.create(
                        sqsService.pushToInputQueue(codeSqsDto)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayInputEvent> captor =
                ArgumentCaptor.forClass(GatewayInputEvent.class);

        verify(internalQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayInputEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals("NR_GATEWAY_INPUT", event.getHeader().getEventType());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(codeSqsDto, event.getPayload());

        verifyNoInteractions(outputQueueMomProducer);
        verifyNoInteractions(internalDlqQueueMomProducer);
    }

    @Test
    void testPushToInputDlqQueue() {
        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder()
                .correlationId("correlationId")
                .pnNationalRegistriesCxId("clientId")
                .build();

        StepVerifier.create(
                        sqsService.pushToInputDlqQueue(codeSqsDto)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayInputEvent> captor =
                ArgumentCaptor.forClass(GatewayInputEvent.class);

        verify(internalDlqQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayInputEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals("NR_GATEWAY_INPUT", event.getHeader().getEventType());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(codeSqsDto, event.getPayload());

        verifyNoInteractions(outputQueueMomProducer);
        verifyNoInteractions(internalQueueMomProducer);
    }
}