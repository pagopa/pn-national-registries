package it.pagopa.pn.national.registries.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class OutputQueueMomProducer extends AbstractSqsMomProducer<GatewayOutputEvent> {

    public OutputQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<GatewayOutputEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
