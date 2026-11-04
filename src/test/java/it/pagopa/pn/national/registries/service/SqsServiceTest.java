package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.OutputQueueMomProducer;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;

import static it.pagopa.pn.national.registries.service.SqsService.NR_GATEWAY_INPUT;
import static it.pagopa.pn.national.registries.service.SqsService.NR_GATEWAY_OUTPUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqsServiceTest {

    private OutputQueueMomProducer outputQueueMomProducer;
    private InternalQueueMomProducer internalQueueMomProducer;
    private InternalDlqQueueMomProducer internalDlqQueueMomProducer;

    private SqsService sqsService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        outputQueueMomProducer = mock(OutputQueueMomProducer.class);
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

        String clientId = "clientId";

        StepVerifier.create(
                        sqsService.pushToOutputQueue(codeSqsDto, clientId)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayOutputEvent> captor =
                ArgumentCaptor.forClass(GatewayOutputEvent.class);

        verify(outputQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayOutputEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals(NR_GATEWAY_OUTPUT, event.getHeader().getEventType());
        assertEquals(clientId, event.getHeader().getClientId());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(codeSqsDto, event.getPayload());

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
                        sqsService.pushToInputQueue(codeSqsDto, clientId)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayInputEvent> captor =
                ArgumentCaptor.forClass(GatewayInputEvent.class);

        verify(internalQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayInputEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals(NR_GATEWAY_INPUT, event.getHeader().getEventType());
        assertEquals(clientId, event.getHeader().getClientId());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(codeSqsDto, event.getPayload());

        verifyNoInteractions(outputQueueMomProducer);
        verifyNoInteractions(internalDlqQueueMomProducer);
    }

    @Test
    void testPushToInputDlqQueue() {
        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder()
                .correlationId("correlationId")
                .build();

        String clientId = "clientId";

        StepVerifier.create(
                        sqsService.pushToInputDlqQueue(codeSqsDto, clientId)
                )
                .verifyComplete();

        ArgumentCaptor<GatewayInputEvent> captor =
                ArgumentCaptor.forClass(GatewayInputEvent.class);

        verify(internalDlqQueueMomProducer, times(1))
                .push(captor.capture());

        GatewayInputEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertEquals(NR_GATEWAY_INPUT, event.getHeader().getEventType());
        assertEquals(clientId, event.getHeader().getClientId());
        assertNotNull(event.getHeader().getCreatedAt());

        assertSame(codeSqsDto, event.getPayload());

        verifyNoInteractions(outputQueueMomProducer);
        verifyNoInteractions(internalQueueMomProducer);
    }
}