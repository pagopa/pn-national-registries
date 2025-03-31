package it.pagopa.pn.national.registries.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.service.GatewayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@lombok.CustomLog
@RequiredArgsConstructor
public class GatewayInputsHandler {
    private final GatewayService gatewayService;

    private static final String HANDLER_PROCESS = "pnNationalRegistriesGatewayRequestConsumer";
    private static final String HANDLER_PROCESS_MULTI_REQUEST = "pnNationalRegistriesGatewayMultiRequestConsumer";

    @Bean
    public Consumer<Message<PnAddressGatewayEvent.Payload>> pnNationalRegistriesGatewayRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_PROCESS);
            log.debug(HANDLER_PROCESS + "- message received for correlationId: {}", message.getPayload().getCorrelationId());
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getCorrelationId());

            var monoResult = gatewayService.handleMessage(message.getPayload())
                    .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_PROCESS))
                    .doOnError(e -> {
                        log.logEndingProcess(HANDLER_PROCESS, false, e.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), e);
                    });

            MDCUtils.addMDCToContextAndExecute(monoResult).block();
        };
    }

    @Bean
    public Consumer<Message<PnAddressesGatewayEvent.Payload>> pnNationalRegistriesGatewayMultiRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_PROCESS_MULTI_REQUEST);
            log.debug(HANDLER_PROCESS_MULTI_REQUEST + "- message received for correlationId: {}", message.getPayload().getCorrelationId());
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getCorrelationId());

            var monoResult = gatewayService.handleMessageMultiRequest(message.getPayload())
                    .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_PROCESS_MULTI_REQUEST))
                    .doOnError(e -> {
                        log.logEndingProcess(HANDLER_PROCESS_MULTI_REQUEST, false, e.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), e);
                    });

            MDCUtils.addMDCToContextAndExecute(monoResult).block();
        };
    }


}
