package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayEventHeader;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayInputEvent;
import it.pagopa.pn.national.registries.middleware.queue.model.GatewayOutputEvent;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalDlqQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.national.registries.middleware.queue.producer.OutputQueueMomProducer;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INIPEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsService {

    public static final String NR_GATEWAY_OUTPUT = "NR_GATEWAY_OUTPUT";
    public static final String NR_GATEWAY_INPUT = "NR_GATEWAY_INPUT";
    private final OutputQueueMomProducer outputQueueMomProducer;
    private final InternalQueueMomProducer internalQueueMomProducer;
    private final InternalDlqQueueMomProducer internalDlqQueueMomProducer;
    private final ObjectMapper mapper;

    public Mono<Void> pushToOutputQueue(CodeSqsDto codeSqsDto, String clientId) {
        GatewayEventHeader header = GatewayEventHeader.builder()
                .createdAt(Instant.now())
                .eventType(NR_GATEWAY_OUTPUT)
                .clientId(clientId)
                .build();

        var outputEvent = new GatewayOutputEvent(header, codeSqsDto);
        return Mono.fromRunnable(() -> outputQueueMomProducer.push(outputEvent)).then();
    }

    public Mono<Void> pushToInputQueue(InternalCodeSqsDto internalCodeSqsDto, String clientId) {
        GatewayEventHeader header = GatewayEventHeader.builder()
                .createdAt(Instant.now())
                .eventType(NR_GATEWAY_INPUT)
                .clientId(clientId)
                .build();

        var inputEvent = new GatewayInputEvent(header, internalCodeSqsDto);
        return Mono.fromRunnable(() -> internalQueueMomProducer.push(inputEvent)).then();

    }

    public Mono<Void> pushToInputDlqQueue(InternalCodeSqsDto internalCodeSqsDto, String clientId) {
        GatewayEventHeader header = GatewayEventHeader.builder()
                .createdAt(Instant.now())
                .eventType(NR_GATEWAY_INPUT)
                .clientId(clientId)
                .build();

        var inputDlqEvent = new GatewayInputEvent(header, internalCodeSqsDto);
        return Mono.fromRunnable(() -> internalDlqQueueMomProducer.push(inputDlqEvent)).then();

    }

    protected <T>T toObject(String msg, Class<T> targetClass) {
        try {
            return mapper.readValue(msg, targetClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }
}
