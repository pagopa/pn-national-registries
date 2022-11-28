package it.pagopa.pn.national.registries.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Map;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_INI_PEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_INI_PEC;


@Slf4j
@Component
public class SqsService {

    private final AmazonSQS amazonSQS;
    private final ObjectMapper mapper;
    private final String queueUrl;

    public SqsService(@Value("${}")String queueUrl, AmazonSQS amazonSQS, ObjectMapper mapper) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.queueUrl = queueUrl;
    }

    public Mono<SendMessageResult> push(CodeSqsDto msges) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setQueueUrl(amazonSQS.getQueueUrl(queueUrl).getQueueUrl());
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setStringValue(msges.getCorrelationId());
        sendMessageRequest.setMessageAttributes(Map.of("correlationId",messageAttributeValue));
        sendMessageRequest.setMessageBody(toJson(msges));
        return Mono.fromCallable(() -> amazonSQS.sendMessage(sendMessageRequest));
    }

    private String toJson(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, e);
        }
    }
}
