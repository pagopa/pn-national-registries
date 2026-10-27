package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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

import static org.junit.jupiter.api.Assertions.*;
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
        assertEquals("{\"correlationId\":\"correlationId\"}", request.messageBody());

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

    @Test
    void pushWithoutClientIdShouldOmitClientIdAttribute() {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );

        StepVerifier.create(sqsService.push("message", null, "queueName", "eventType"))
                .expectNext(sendMessageResponse)
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> sendMessageCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSQS).sendMessage(sendMessageCaptor.capture());
        SendMessageRequest request = sendMessageCaptor.getValue();
        assertFalse(request.messageAttributes().containsKey("clientId"));
        assertEquals("eventType", request.messageAttributes().get("eventType").stringValue());
        assertEquals("message", request.messageBody());
    }

    @Test
    void pushShouldPropagateGetQueueUrlError() {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );
        RuntimeException error = new RuntimeException("get queue url failed");
        when(amazonSQS.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(error));

        StepVerifier.create(sqsService.push("message", CLIENT_ID, "queueName", "eventType"))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();

        verify(amazonSQS, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void pushShouldPropagateSendMessageError() {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );
        RuntimeException error = new RuntimeException("send failed");
        when(amazonSQS.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(error));

        StepVerifier.create(sqsService.push("message", CLIENT_ID, "queueName", "eventType"))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();
    }

    @Test
    void toJsonShouldThrowPnInternalExceptionWhenSerializationFails() throws Exception {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failed") { });

        assertThrows(PnInternalException.class, () -> sqsService.toJson(new CodeSqsDto()));
    }

    @Test
    void toObjectShouldDeserializeMessage() throws Exception {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );
        CodeSqsDto expected = new CodeSqsDto();
        when(objectMapper.readValue("message", CodeSqsDto.class)).thenReturn(expected);

        CodeSqsDto result = sqsService.toObject("message", CodeSqsDto.class);

        assertSame(expected, result);
        verify(objectMapper).readValue("message", CodeSqsDto.class);
    }

    @Test
    void toObjectShouldThrowPnInternalExceptionWhenDeserializationFails() throws Exception {
        SqsService sqsService = new SqsService(
                amazonSQS,
                objectMapper,
                nationalRegistriesConfig
        );
        when(objectMapper.readValue("message", CodeSqsDto.class))
                .thenThrow(new JsonProcessingException("deserialization failed") { });

        assertThrows(PnInternalException.class, () -> sqsService.toObject("message", CodeSqsDto.class));
    }
}