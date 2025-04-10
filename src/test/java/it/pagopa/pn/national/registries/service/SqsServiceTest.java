package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiCodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiRecipientCodeSqsDto;
import org.junit.jupiter.api.BeforeEach;
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

    private SqsService sqsService;

    @BeforeEach
    void init() {
        SqsClient amazonSQS = mock(SqsClient.class);
        GetQueueUrlResponse getQueueUrlResponse = GetQueueUrlResponse.builder().queueUrl("queueUrl").build();
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);
        when(amazonSQS.sendMessage((SendMessageRequest) any())).thenReturn(sendMessageResponse);

        ObjectMapper objectMapper = mock(ObjectMapper.class);
        this.sqsService = new SqsService(
                "outputQueue",
                "inputQueue",
                "validationInputQueueName",
                "inputDqlQueue",
                "validationInputDlqQueue",
                amazonSQS,
                objectMapper
        );
    }

    @Test
    void testPushToOutputQueue() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToOutputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToOutputQueue_MultiRequest() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        MultiCodeSqsDto codeSqsDto = new MultiCodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushMultiToOutputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
    @Test
    void testPushToInputQueue() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        InternalCodeSqsDto codeSqsDto =InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToMultiInputQueue() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        MultiRecipientCodeSqsDto codeSqsDto = MultiRecipientCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToValidationInputQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToInputDlqQueue() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        InternalCodeSqsDto codeSqsDto = InternalCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputDlqQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }

    @Test
    void testPushToInputDlqQueue_MultiRequest() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();

        MultiRecipientCodeSqsDto codeSqsDto = MultiRecipientCodeSqsDto.builder().build();
        codeSqsDto.setCorrelationId("correlationId");
        StepVerifier.create(sqsService.pushToInputDlqQueue(codeSqsDto,"clientId"))
                .expectNext(sendMessageResponse)
                .verifyComplete();
    }
}
