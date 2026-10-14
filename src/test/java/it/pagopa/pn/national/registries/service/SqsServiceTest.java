package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SqsServiceTest {

    private static final String CLIENT_ID = "clientId";
    private static final String CORRELATION_ID = "correlationId";
    private static final String QUEUE_URL = "queueUrl";

    private SqsAsyncClient amazonSQS;
    private ObjectMapper objectMapper;
    private NationalRegistriesConfig nationalRegistriesConfig;
    private NationalRegistriesConfig.Sqs sqs;

    private SendMessageResponse sendMessageResponse;

    @BeforeEach
    void setUp() throws Exception {
        amazonSQS = mock(SqsAsyncClient.class);
        objectMapper = mock(ObjectMapper.class);

        nationalRegistriesConfig = new NationalRegistriesConfig();
        sqs = new NationalRegistriesConfig.Sqs();
        nationalRegistriesConfig.setSqs(sqs);

        sendMessageResponse = SendMessageResponse.builder().build();

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"correlationId\":\"correlationId\"}");

        when(amazonSQS.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                GetQueueUrlResponse.builder()
                                        .queueUrl(QUEUE_URL)
                                        .build()
                        )
                );

        when(amazonSQS.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(
                        CompletableFuture.completedFuture(sendMessageResponse)
                );
    }

    @Test
    void testPushToOutputQueue() {
        sqs.setOutputQueueName("outputQueueName");

        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(CORRELATION_ID);

        StepVerifier.create(
                        sqsService.pushToOutputQueue(codeSqsDto, CLIENT_ID)
                )
                .expectNext(sendMessageResponse)
                .verifyComplete();

        ArgumentCaptor<GetQueueUrlRequest> getQueueUrlCaptor =
                ArgumentCaptor.forClass(GetQueueUrlRequest.class);

        ArgumentCaptor<SendMessageRequest> sendMessageCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(amazonSQS).getQueueUrl(getQueueUrlCaptor.capture());
        verify(amazonSQS).sendMessage(sendMessageCaptor.capture());

        assertEquals(
                "outputQueueName",
                getQueueUrlCaptor.getValue().queueName()
        );

        SendMessageRequest request = sendMessageCaptor.getValue();

        assertEquals(QUEUE_URL, request.queueUrl());
        assertNotNull(request.messageBody());

        assertEquals(
                CLIENT_ID,
                request.messageAttributes()
                        .get("clientId")
                        .stringValue()
        );

        assertEquals(
                "NR_GATEWAY_RESPONSE",
                request.messageAttributes()
                        .get("eventType")
                        .stringValue()
        );
    }

    @Test
    void testPushToInputQueue() {
        sqs.setInputQueueName("inputQueueName");

        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );

        InternalCodeSqsDto codeSqsDto =
                InternalCodeSqsDto.builder().build();

        codeSqsDto.setCorrelationId(CORRELATION_ID);

        StepVerifier.create(
                        sqsService.pushToInputQueue(codeSqsDto, CLIENT_ID)
                )
                .expectNext(sendMessageResponse)
                .verifyComplete();

        ArgumentCaptor<GetQueueUrlRequest> getQueueUrlCaptor =
                ArgumentCaptor.forClass(GetQueueUrlRequest.class);

        ArgumentCaptor<SendMessageRequest> sendMessageCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(amazonSQS).getQueueUrl(getQueueUrlCaptor.capture());
        verify(amazonSQS).sendMessage(sendMessageCaptor.capture());

        assertEquals(
                "inputQueueName",
                getQueueUrlCaptor.getValue().queueName()
        );

        SendMessageRequest request = sendMessageCaptor.getValue();

        assertEquals(QUEUE_URL, request.queueUrl());
        assertNotNull(request.messageBody());

        assertEquals(
                CLIENT_ID,
                request.messageAttributes()
                        .get("clientId")
                        .stringValue()
        );

        assertEquals(
                "NR_GATEWAY_INPUT",
                request.messageAttributes()
                        .get("eventType")
                        .stringValue()
        );
    }

    @Test
    void testPushToInputDlqQueue() {
        sqs.setInputDlqQueueName("inputDlqQueueName");

        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );

        InternalCodeSqsDto codeSqsDto =
                InternalCodeSqsDto.builder().build();

        codeSqsDto.setCorrelationId(CORRELATION_ID);

        StepVerifier.create(
                        sqsService.pushToInputDlqQueue(codeSqsDto, CLIENT_ID)
                )
                .expectNext(sendMessageResponse)
                .verifyComplete();

        ArgumentCaptor<GetQueueUrlRequest> getQueueUrlCaptor =
                ArgumentCaptor.forClass(GetQueueUrlRequest.class);

        ArgumentCaptor<SendMessageRequest> sendMessageCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(amazonSQS).getQueueUrl(getQueueUrlCaptor.capture());
        verify(amazonSQS).sendMessage(sendMessageCaptor.capture());

        assertEquals(
                "inputDlqQueueName",
                getQueueUrlCaptor.getValue().queueName()
        );

        SendMessageRequest request = sendMessageCaptor.getValue();

        assertEquals(QUEUE_URL, request.queueUrl());
        assertNotNull(request.messageBody());

        assertEquals(
                CLIENT_ID,
                request.messageAttributes()
                        .get("clientId")
                        .stringValue()
        );

        assertEquals(
                "NR_GATEWAY_INPUT",
                request.messageAttributes()
                        .get("eventType")
                        .stringValue()
        );
    }
}