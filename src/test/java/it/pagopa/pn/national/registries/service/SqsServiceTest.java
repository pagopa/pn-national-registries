package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqsServiceTest {

    @Test
    void testPush() {
        SqsClient amazonSQS = mock(SqsClient.class);
        GetQueueUrlResponse getQueueUrlResponse = GetQueueUrlResponse.builder().queueUrl("queueUrl").build();
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponse);

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SqsService sqsService = new SqsService("queueNameTest", amazonSQS, objectMapper);

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.push(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
}
