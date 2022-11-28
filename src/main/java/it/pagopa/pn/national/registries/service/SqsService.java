package it.pagopa.pn.national.registries.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;


@Slf4j
@Component
public class SqsService {

    private final AmazonSQS amazonSQS;
    private final ObjectMapper mapper;

    public SqsService(AmazonSQS amazonSQS, ObjectMapper mapper) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public Mono<SendMessageResult> push(CodeSqsDto msges) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setQueueUrl("pn-inipec");
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setStringValue(msges.getCorrelationId());
        sendMessageRequest.setMessageAttributes(Map.of("correlationId",messageAttributeValue));
        sendMessageRequest.setMessageBody(toJson(msges));
        return Mono.fromCallable(() -> amazonSQS.sendMessage(sendMessageRequest));
    }

    private String toJson(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException exc) {
            //throw new PnNationalRegistriesException();
        }
        return null;
    }

}
