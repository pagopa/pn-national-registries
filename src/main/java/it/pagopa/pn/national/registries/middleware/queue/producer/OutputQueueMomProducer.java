package it.pagopa.pn.national.registries.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class OutputQueueMomProducer extends AbstractGatewayQueueMomProducer<GatewayOutputEvent> {

    public OutputQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<GatewayOutputEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}