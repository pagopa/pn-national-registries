package it.pagopa.pn.national.registries.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


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
        return sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName("pn-inipec").build())
                .queueUrl();
    }

    public void push(List<List<CodeSqsDto>> msges) {
        for(List<CodeSqsDto> ms : msges){
            for (int i = 0; i < ms.size(); i += 10) {
                List<CodeSqsDto> sub = ms.subList(i, Math.min(ms.size(),i+10));
                sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                        .queueUrl(this.queueUrl)
                        .entries(sub.stream()
                                .map(msg -> SendMessageBatchRequestEntry.builder()
                                        .messageBody(toJson(msg))
                                        .id(UUID.randomUUID().toString())
                                        .build()
                                )
                                .collect(Collectors.toList()))
                        .build());
            }
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
