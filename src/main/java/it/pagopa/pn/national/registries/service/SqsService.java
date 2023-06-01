package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INIPEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC;

@Slf4j
@Component
public class SqsService {

    private final SqsClient sqsClient;
    private final ObjectMapper mapper;
    private final String queueName;

    public SqsService(@Value("${pn.national.registries.sqs.queue.name}") String queueName,
                      SqsClient sqsClient,
                      ObjectMapper mapper) {
        this.sqsClient = sqsClient;
        this.mapper = mapper;
        this.queueName = queueName;
    }

    public Mono<SendMessageResponse> push(CodeSqsDto msg, String pnNationalRegistriesCxId) {
        log.info("pushing message for clientId: [{}] with correlationId: {}", pnNationalRegistriesCxId, msg.getCorrelationId());
        log.debug("Inserting data {} in SQS {}", msg,queueName);
        log.info("Inserted data in SQS {}",queueName);
        return push(toJson(msg), pnNationalRegistriesCxId);
    }

    public Mono<SendMessageResponse> push(String msg, String pnNationalRegistriesCxId) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageAttributes(buildMessageAttributeMap(pnNationalRegistriesCxId))
                .messageBody(msg)
                .build();

        return Mono.fromCallable(() -> {
            log.info("pushed message to queue: {} / {}", queueName, queueUrl);
            return sqsClient.sendMessage(sendMsgRequest);
        });
    }

    private Map<String, MessageAttributeValue> buildMessageAttributeMap(String pnNationalRegistriesCxId) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        if (StringUtils.hasText(pnNationalRegistriesCxId)) {
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnNationalRegistriesCxId).dataType("String").build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue("NR_GATEWAY_RESPONSE").dataType("String").build());
        return attributes;
    }

    private String toJson(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }
}
