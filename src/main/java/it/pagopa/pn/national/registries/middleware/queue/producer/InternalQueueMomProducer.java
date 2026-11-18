package it.pagopa.pn.national.registries.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class InternalQueueMomProducer extends AbstractSqsMomProducer<GatewayInputEvent> {

    public InternalQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<GatewayInputEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }

}
