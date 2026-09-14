package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqsServiceTest {

    @Test
    void testPushToOutputQueue() {
        SqsAsyncClient amazonSQS = mock(SqsAsyncClient.class);
        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);

        when(amazonSQS.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponseCompletableFuture);

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SqsService sqsService = new SqsService("queueNameTest", "inputQueue", "inputDqlQueue", amazonSQS, objectMapper);

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToOutputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
    @Test
    void testPushToInputQueue() {
        SqsAsyncClient amazonSQS = mock(SqsAsyncClient.class);
        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponseCompletableFuture);

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SqsService sqsService = new SqsService("queueNameTest", "inputQueue", "inputDqlQueue", amazonSQS, objectMapper);

        InternalCodeSqsDto codeSqsDto =InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToInputDlqQueue() {
        SqsAsyncClient amazonSQS = mock(SqsAsyncClient.class);
        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponseCompletableFuture);

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SqsService sqsService = new SqsService("queueNameTest", "inputQueue", "inputDqlQueue", amazonSQS, objectMapper);

        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputDlqQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
}
