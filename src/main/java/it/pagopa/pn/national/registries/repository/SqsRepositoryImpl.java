package it.pagopa.pn.national.registries.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;


@Slf4j
@Component
public class SqsRepositoryImpl implements SqsRepository {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper mapper;

    public SqsRepositoryImpl(SqsClient sqsClient, ObjectMapper mapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = getQueueUrl(sqsClient);
        this.mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private String getQueueUrl(SqsClient sqsClient) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("pn-inipec").build()).queueUrl();
    }

    public void push(List<CodeSqsDto> msges) {
        for (int i = 0; i < msges.size(); i += 10) {
            List<CodeSqsDto> sub = msges.subList(i, Math.min(msges.size(),i+10));
            sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                    .queueUrl(this.queueUrl)
                    .entries(sub.stream()
                            .map(msg -> SendMessageBatchRequestEntry.builder()
                                    .messageBody(toJson(msg))
                                    .id(msg.getCorrelationId())
                                    .build()
                            )
                            .collect(Collectors.toList()))
                    .build());
        }
    }

    private String toJson(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException(exc);
        }
    }

}
