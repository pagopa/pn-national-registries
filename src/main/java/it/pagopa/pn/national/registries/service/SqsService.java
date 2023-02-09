package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
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

    private final SqsClient sqsClient;
    private final ObjectMapper mapper;
    private final String queueName;

    public SqsService(@Value("${pn.national.registries.sqs.queue.name}") String queueName,
                      SqsClient sqsClient,
                      ObjectMapper mapper) {
        this.sqsClient = sqsClient;
        this.mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.queueName = queueName;
    }

    public Mono<SendMessageResponse> push(CodeSqsDto msg, String pnNationalRegistriesCxId) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        log.info("Creating MessageRequest for taxId: {} and correlationId: {}", MaskDataUtils.maskString(msg.getTaxId()), msg.getCorrelationId());
        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageAttributes(buildMessageAttributeMap(pnNationalRegistriesCxId))
                .messageBody(toJson(msg))
                .build();

        log.info("Created MessageRequest for taxId: {} and correlationId: {}", MaskDataUtils.maskString(msg.getTaxId()), msg.getCorrelationId());

        return Mono.fromCallable(() -> sqsClient.sendMessage(sendMsgRequest));
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
