package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsServiceTest {

    @InjectMocks
    private SqsService sqsService;

    @Mock
    SqsAsyncClient amazonSQS;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    ObjectMapper objectMapper;

    @Test
    void testPushToOutputQueue() {
        when(nationalRegistriesConfig.getOutputQueueName()).thenReturn("outputQueueName");

        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);

        when(amazonSQS.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponseCompletableFuture);

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToOutputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
    @Test
    void testPushToInputQueue() {
        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);
        when(nationalRegistriesConfig.getInputQueueName()).thenReturn("inputQueueName");

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponseCompletableFuture);

        InternalCodeSqsDto codeSqsDto =InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToInputDlqQueue() {
        when(nationalRegistriesConfig.getInputDlqQueueName()).thenReturn("inputDlqQueueName");

        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponse = CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("queueUrl").build());
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
        CompletableFuture<SendMessageResponse> sendMessageResponseCompletableFuture = CompletableFuture.completedFuture(sendMessageResponse);

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponseCompletableFuture);

        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputDlqQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
}
