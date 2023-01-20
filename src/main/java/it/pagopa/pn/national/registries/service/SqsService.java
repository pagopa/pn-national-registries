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
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_INI_PEC;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_INI_PEC;


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

    public Mono<SendMessageResponse> push(CodeSqsDto msg) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        log.info("Creating MessageRequest for taxId: {} and correlationId: {}", MaskDataUtils.maskString(msg.getTaxId()), msg.getCorrelationId());
        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(toJson(msg))
                .build();
        log.info("Created MessageRequest for taxId: {} and correlationId: {}", MaskDataUtils.maskString(msg.getTaxId()), msg.getCorrelationId());

        return Mono.fromCallable(() -> sqsClient.sendMessage(sendMsgRequest));
    }

    private String toJson(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, e);
        }
    }
}
