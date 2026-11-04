package it.pagopa.pn.national.registries.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.OutputQueueMomProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnNationalRegistriesMiddlewareConfig {

    private final String outputQueueName;
    private final String inputQueueName;
    private final String inputDlqQueueName;

    public PnNationalRegistriesMiddlewareConfig(@Value("${pn.national.registries.sqs.output.queue.name}") String outputQueueName,
                      @Value("${pn.national.registries.sqs.input.queue.name}") String inputQueueName,
                      @Value("${pn.national.registries.sqs.input.dlq.queue.name}") String inputDlqQueueName) {
        this.outputQueueName = outputQueueName;
        this.inputQueueName = inputQueueName;
        this.inputDlqQueueName = inputDlqQueueName;
    }


    @Bean
    public OutputQueueMomProducer outputQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new OutputQueueMomProducer( sqs, outputQueueName, objMapper, GatewayOutputEvent.class);
    }

    @Bean
    public InternalQueueMomProducer internalQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new InternalQueueMomProducer( sqs, inputQueueName, objMapper, GatewayInputEvent.class);
    }

    @Bean
    public InternalDlqQueueMomProducer internalDlqQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new InternalDlqQueueMomProducer( sqs, inputDlqQueueName, objMapper, GatewayInputEvent.class);
    }
}

