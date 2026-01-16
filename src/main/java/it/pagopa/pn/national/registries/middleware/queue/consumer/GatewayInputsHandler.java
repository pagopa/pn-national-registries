package it.pagopa.pn.national.registries.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.service.GatewayService;
import it.pagopa.pn.national.registries.utils.ConsumerMDCUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@lombok.CustomLog
@RequiredArgsConstructor
public class GatewayInputsHandler {
    private final GatewayService gatewayService;

    private static final String HANDLER_PROCESS = "pnNationalRegistriesGatewayRequestConsumer";

    @SqsListener(value = "${pn.national.registries.sqs.input.queue.name}")
    public void pnNationalRegistriesGatewayRequestConsumer(Message<PnAddressGatewayEvent.Payload> message) {
        log.logStartingProcess(HANDLER_PROCESS);
        log.debug(HANDLER_PROCESS + "- message received for correlationId: {}", message.getPayload().getCorrelationId());
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getCorrelationId());

        var monoResult = gatewayService.handleMessage(message.getPayload())
                .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_PROCESS))
                .doOnError(e -> {
                    log.logEndingProcess(HANDLER_PROCESS, false, e.getMessage());
                    HandleEventUtils.handleException(message.getHeaders(), e);
                });

        MDCUtils.addMDCToContextAndExecute(monoResult).block();
    }

}
