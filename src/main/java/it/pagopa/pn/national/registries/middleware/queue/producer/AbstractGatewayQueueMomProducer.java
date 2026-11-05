package it.pagopa.pn.national.registries.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayEventHeader;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public abstract class AbstractGatewayQueueMomProducer<T extends GenericEvent> extends AbstractSqsMomProducer<T> {

    protected AbstractGatewayQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<T> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }

    @Override
    protected Map<String, MessageAttributeValue> getSqSHeader(GenericEventHeader header) {
        Map<String, MessageAttributeValue> map = super.getSqSHeader(header);

        if (!(header instanceof GatewayEventHeader gatewayHeader)) {
            return map;
        }

        map.put("clientId", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(String.valueOf(gatewayHeader.getClientId()))
                        .build());

        return map;
    }
}