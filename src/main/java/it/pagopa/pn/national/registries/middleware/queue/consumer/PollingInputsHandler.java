package it.pagopa.pn.national.registries.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import it.pagopa.pn.national.registries.utils.ConsumerMDCUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_INVALIDREGISTRYFORPOLLING;

@Component
@lombok.CustomLog
@RequiredArgsConstructor
public class PollingInputsHandler {

    private final InipecPollingService inipecPollingService;
    private final InadPollingService inadPollingService;

    private static final String HANDLER_PROCESS = "pnNationalRegistriesPollingInputsConsumer";

    @SqsListener(value = "${pn.national-registries.sqs.pollingqueuename}")
    public void pnNationalRegistriesPollingInputsConsumer(Message<PnPollingEvent.Payload> message) {
        log.logStartingProcess(HANDLER_PROCESS);
        if(log.isDebugEnabled()) {
            log.debug(HANDLER_PROCESS + " - Message from pollingQueue, payload: {}", message.getPayload());
        }
        else {
            log.info(HANDLER_PROCESS + "- Message from pollingQueue, batchId: {}", message.getPayload().getBatchId());
        }
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getBatchId());

        GatewayDownstreamService gatewayDownstreamService = message.getPayload().getRegistry();
        var result = switch (gatewayDownstreamService) {
            case INIPEC -> inipecPollingService.handleMessage(message.getPayload());
            case INAD -> inadPollingService.handleMessage(message.getPayload());
            default -> {
                log.warn("Invalid registry: {} polling is not enabled", gatewayDownstreamService);
                yield Mono.error(new PnInternalException("Invalid registry: " + gatewayDownstreamService + " polling is not enabled", ERROR_CODE_NATIONAL_REGISTRIES_INVALIDREGISTRYFORPOLLING));
            }
        };

        MDCUtils.addMDCToContextAndExecute(result).block();
    }
}
