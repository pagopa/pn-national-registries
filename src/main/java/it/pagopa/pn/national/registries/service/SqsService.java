package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayResponseEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.GatewayResponseQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INIPEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsService {

    private static final String NR_GATEWAY_RESPONSE = "NR_GATEWAY_RESPONSE";
    private static final String NR_GATEWAY_INPUT = "NR_GATEWAY_INPUT";
    private static final String PN_NATIONAL_REGISTRIES = "pn-national-registries";
    private final GatewayResponseQueueMomProducer outputQueueMomProducer;
    private final InternalQueueMomProducer internalQueueMomProducer;
    private final InternalDlqQueueMomProducer internalDlqQueueMomProducer;
    private final ObjectMapper mapper;

    public Mono<Void> pushToOutputQueue(GatewayResponse gatewayResponse) {
        GenericEventHeader header = buildHeader(NR_GATEWAY_RESPONSE);
        var outputEvent = new GatewayResponseEvent(header, gatewayResponse);
        return Mono.fromRunnable(() -> outputQueueMomProducer.push(outputEvent))
                .doOnSuccess(unused -> log.info("Message pushed to output queue for correlationId: {}", gatewayResponse.getPayload().getCorrelationId()))
                .doOnError(error -> log.error("Failed to push message to output queue for correlationId: {}", gatewayResponse.getPayload().getCorrelationId(), error))
                .then();
    }

    public Mono<Void> pushToInputQueue(InternalCodeSqsDto internalCodeSqsDto) {
        GenericEventHeader header = buildHeader(NR_GATEWAY_INPUT);
        var inputEvent = new GatewayInputEvent(header, internalCodeSqsDto);
        return Mono.fromRunnable(() -> internalQueueMomProducer.push(inputEvent))
                .doOnSuccess(unused -> log.info("Message pushed to input queue for correlationId: {}", internalCodeSqsDto.getCorrelationId()))
                .doOnError(error -> log.error("Failed to push message to input queue for correlationId: {}", internalCodeSqsDto.getCorrelationId(), error))
                .then();
    }

    public Mono<Void> pushToInputDlqQueue(InternalCodeSqsDto internalCodeSqsDto) {
        GenericEventHeader header = buildHeader(NR_GATEWAY_INPUT);
        var inputDlqEvent = new GatewayInputEvent(header, internalCodeSqsDto);
        return Mono.fromRunnable(() -> internalDlqQueueMomProducer.push(inputDlqEvent))
                .doOnSuccess(unused -> log.info("Message pushed to input DLQ queue for correlationId: {}", internalCodeSqsDto.getCorrelationId()))
                .doOnError(error -> log.error("Failed to push message to input DLQ for correlationId: {}", internalCodeSqsDto.getCorrelationId(), error))
                .then();
    }

    private static GenericEventHeader buildHeader(String nrGatewayResponse) {
        return GenericEventHeader.builder()
                .publisher(PN_NATIONAL_REGISTRIES)
                .createdAt(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType(nrGatewayResponse)
                .build();
    }

    protected <T>T toObject(String msg, Class<T> targetClass) {
        try {
            return mapper.readValue(msg, targetClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }
}
