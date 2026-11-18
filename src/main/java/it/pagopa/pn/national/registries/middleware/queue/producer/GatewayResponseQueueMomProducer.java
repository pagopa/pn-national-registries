package it.pagopa.pn.national.registries.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayResponseEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class GatewayResponseQueueMomProducer extends AbstractSqsMomProducer<GatewayResponseEvent> {

    public GatewayResponseQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<GatewayResponseEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}