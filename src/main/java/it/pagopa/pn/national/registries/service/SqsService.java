package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiCodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiRecipientCodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INIPEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC;

@Slf4j
@Component
public class SqsService {

    private static final String PUSHING_MESSAGE = "pushing message for clientId: [{}] with correlationId: {}";
    private static final String INSERTING_MSG_WITHOUT_DATA = "Inserted data in SQS {}";

    private final SqsClient sqsClient;
    private final ObjectMapper mapper;
    private final String outputQueueName;
    private final String inputQueueName;
    private final String inputDlqQueueName;
    private final String validationInputQueueName;

    public SqsService(@Value("${pn.national.registries.sqs.output.queue.name}") String outputQueueName,
                      @Value("${pn.national.registries.sqs.input.queue.name}") String inputQueueName,
                      @Value("${pn.national.registries.sqs.input.validation.queue.name}") String validationInputQueueName,
                      @Value("${pn.national.registries.sqs.input.dlq.queue.name}") String inputDlqQueueName,
                      SqsClient sqsClient,
                      ObjectMapper mapper) {
        this.sqsClient = sqsClient;
        this.mapper = mapper;
        this.outputQueueName = outputQueueName;
        this.inputQueueName = inputQueueName;
        this.inputDlqQueueName = inputDlqQueueName;
        this.validationInputQueueName = validationInputQueueName;
    }

    public Mono<SendMessageResponse> pushToOutputQueue(CodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, outputQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, outputQueueName, "NR_GATEWAY_RESPONSE");
    }

    public Mono<SendMessageResponse> pushMultiToOutputQueue(MultiCodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, outputQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, outputQueueName, "NR_GATEWAY_RESPONSE");
    }

    public Mono<SendMessageResponse> pushToInputQueue(InternalCodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, inputQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, inputQueueName, "NR_GATEWAY_INPUT");
    }

    public Mono<SendMessageResponse> pushToValidationInputQueue(MultiRecipientCodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, validationInputQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, validationInputQueueName, "NR_GATEWAY_MULTI_INPUT");
    }

    public Mono<SendMessageResponse> pushToInputDlqQueue(InternalCodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, inputDlqQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, inputDlqQueueName, "NR_GATEWAY_INPUT");
    }

    public Mono<SendMessageResponse> pushToInputDlqQueue(MultiRecipientCodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info(PUSHING_MESSAGE, pnNationalRegistriesCxId, msg.getCorrelationId());
        log.info(INSERTING_MSG_WITHOUT_DATA, inputDlqQueueName);
        return push(toJson(msg), pnNationalRegistriesCxId, inputDlqQueueName, "NR_GATEWAY_INPUT");
    }

    public Mono<SendMessageResponse> push(String msg, String pnNationalRegistriesCxId, String queueName, String eventType) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageAttributes(buildMessageAttributeMap(pnNationalRegistriesCxId, eventType))
                .messageBody(msg)
                .build();

        return Mono.just(sqsClient.sendMessage(sendMsgRequest));
    }

    private Map<String, MessageAttributeValue> buildMessageAttributeMap(String pnNationalRegistriesCxId, String eventType) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        if (StringUtils.hasText(pnNationalRegistriesCxId)) {
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnNationalRegistriesCxId).dataType("String").build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue(eventType).dataType("String").build());
        return attributes;
    }

    protected String toJson(Object codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }

    protected <T>T toObject(String msg, Class<T> targetClass) {
        try {
            return mapper.readValue(msg, targetClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }
}
