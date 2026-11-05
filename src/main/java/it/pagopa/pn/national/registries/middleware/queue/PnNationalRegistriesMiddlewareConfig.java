package it.pagopa.pn.national.registries.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.OutputQueueMomProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnNationalRegistriesMiddlewareConfig {

    private final NationalRegistriesConfig cfg;

    public PnNationalRegistriesMiddlewareConfig(NationalRegistriesConfig cfg) {
        this.cfg = cfg;
    }

    @Bean
    public OutputQueueMomProducer outputQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new OutputQueueMomProducer( sqs, cfg.getTopic().getOutputQueueName(), objMapper, GatewayOutputEvent.class);
    }

    @Bean
    public InternalQueueMomProducer internalQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new InternalQueueMomProducer( sqs, cfg.getTopic().getInputQueueName(), objMapper, GatewayInputEvent.class);
    }

    @Bean
    public InternalDlqQueueMomProducer internalDlqQueueMomProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new InternalDlqQueueMomProducer( sqs, cfg.getTopic().getInputDlqQueueName(), objMapper, GatewayInputEvent.class);
    }
}

