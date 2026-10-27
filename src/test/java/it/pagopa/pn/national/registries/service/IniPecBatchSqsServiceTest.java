package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "pn.national-registries.inipec.batch.sqs.recovery.delay=30000",
        "pn.national-registries.inipec.batch-request-pk-separator=~"
})
@ContextConfiguration(classes = IniPecBatchSqsService.class)
@ExtendWith(SpringExtension.class)
class IniPecBatchSqsServiceTest {

    private static final String CLIENT_ID = "clientId";
    private static final String MESSAGE = "message";
    private static final String PF_CF = "RSSMRA80A01H501U";
    private static final String PG_CF = "12345678901";
    private static final String CORRELATION_ID = "correlationId";
    private static final String CORRELATION_ID_WITH_SUFFIX = "correlationId~001";

    @Autowired
    private IniPecBatchSqsService iniPecBatchSqsService;

    @MockitoBean
    private IniPecBatchRequestRepository batchRequestRepository;

    @MockitoBean
    private SqsService sqsService;

    @MockitoBean
    private NationalRegistriesConfig nationalRegistriesConfig;

    @BeforeEach
    void setup(){
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setBatchRequestPkSeparator("~");
        when(nationalRegistriesConfig.getInipec()).thenReturn(inipec);
    }

    @Test
    void batchSendToSqs_emptyList_shouldCompleteWithoutInteractions() {

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(Collections.emptyList())).verifyComplete();

        verifyNoInteractions(batchRequestRepository);
        verifyNoInteractions(sqsService);
    }

    @Test
    void batchSendToSqs_shouldSendExpectedPayloadToOutputQueue() {

        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto expectedPayload = mock(CodeSqsDto.class);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(expectedPayload);
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().messageId("message-id").build()));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        ArgumentCaptor<CodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService).pushToOutputQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        assertSame(expectedPayload, payloadCaptor.getValue());
        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getReservationId());
        assertNotNull(batchRequest.getLastReserved());

        verify(sqsService).toObject(MESSAGE, CodeSqsDto.class);
        verify(batchRequestRepository).update(same(batchRequest));
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
    }

    @Test
    void batchSendToSqs_whenReservationFails_shouldSkipMessage() {

        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    void batchSendToSqs_whenOutputQueueFails_shouldKeepNotSent() {

        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto expectedPayload = mock(CodeSqsDto.class);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(expectedPayload);
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("SQS error").build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        ArgumentCaptor<CodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService).pushToOutputQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        assertSame(expectedPayload, payloadCaptor.getValue());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        verify(batchRequestRepository, never()).update(any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
    }

    @Test
    void batchSendToSqs_mixedBatch_shouldHandleAllCases() {

        BatchRequest reservationKo = createOutputBatchRequest(PF_CF);
        BatchRequest sqsKo = createOutputBatchRequest(PF_CF);
        BatchRequest success = createOutputBatchRequest(PF_CF);

        reservationKo.setCorrelationId("correlation-1~001");
        sqsKo.setCorrelationId("correlation-2~001");
        success.setCorrelationId("correlation-3~001");

        CodeSqsDto sqsKoPayload = mock(CodeSqsDto.class);
        CodeSqsDto successPayload = mock(CodeSqsDto.class);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(any())).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()), Mono.just(sqsKo), Mono.just(success));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(sqsKoPayload, successPayload);
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("KO").build()), Mono.just(SendMessageResponse.builder().messageId("OK").build()));
        when(batchRequestRepository.update(same(success))).thenReturn(Mono.just(success));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(reservationKo, sqsKo, success))).verifyComplete();

        ArgumentCaptor<CodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService, times(2)).pushToOutputQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        List<CodeSqsDto> capturedPayloads = payloadCaptor.getAllValues();

        assertEquals(2, capturedPayloads.size());
        assertSame(sqsKoPayload, capturedPayloads.get(0));
        assertSame(successPayload, capturedPayloads.get(1));
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), reservationKo.getSendStatus());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), sqsKo.getSendStatus());
        assertEquals(BatchSendStatus.SENT.getValue(), success.getSendStatus());

        verify(batchRequestRepository).update(same(success));
        verify(batchRequestRepository, never()).update(same(reservationKo));
        verify(batchRequestRepository, never()).update(same(sqsKo));
    }

    @ParameterizedTest(name = "batch status ERROR -> DLQ recipientType {2}")
    @MethodSource("recipientTypeCases")
    void batchSendToSqs_whenBatchStatusIsError_shouldSendExpectedPayloadToDlq(String cf, String inputRecipientType, RecipientType expectedRecipientType) {

        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 10, 30);
        BatchRequest batchRequest = createDlqBatchRequest(cf, inputRecipientType, referenceRequestDate);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().messageId("dlq-message").build()));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        ArgumentCaptor<InternalCodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = payloadCaptor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals(Date.from(referenceRequestDate.atZone(ZoneId.systemDefault()).toInstant()), payload.getReferenceRequestDate());
        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());

        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
        verify(batchRequestRepository).update(same(batchRequest));
    }

    @ParameterizedTest(name = "DLQ failure recipientType {2}")
    @MethodSource("recipientTypeCases")
    void batchSendToSqs_whenDlqFails_shouldPropagateError(String cf, String inputRecipientType, RecipientType expectedRecipientType) {

        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 10, 30);
        BatchRequest batchRequest = createDlqBatchRequest(cf, inputRecipientType, referenceRequestDate);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("DLQ error").build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).expectError(SqsException.class).verify();

        ArgumentCaptor<InternalCodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = payloadCaptor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals(Date.from(referenceRequestDate.atZone(ZoneId.systemDefault()).toInstant()), payload.getReferenceRequestDate());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        verify(batchRequestRepository, never()).update(any());
        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
    }

    @ParameterizedTest(name = "sendToDlqQueue recipientType {2}")
    @MethodSource("recipientTypeCases")
    void sendToDlqQueue_shouldSendExpectedPayload(String cf, String inputRecipientType, RecipientType expectedRecipientType) {

        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 8, 15);
        BatchRequest batchRequest = createBaseRequest(cf, inputRecipientType);

        batchRequest.setReferenceRequestDate(referenceRequestDate);

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(batchRequest)).verifyComplete();

        ArgumentCaptor<InternalCodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = payloadCaptor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(Date.from(referenceRequestDate.atZone(ZoneId.systemDefault()).toInstant()), payload.getReferenceRequestDate());
    }

    @ParameterizedTest(name = "sendToDlqQueue without date recipientType {2}")
    @MethodSource("recipientTypeCases")
    void sendToDlqQueue_whenReferenceRequestDateIsNull_shouldSendPayloadWithoutDate(String cf, String inputRecipientType, RecipientType expectedRecipientType) {

        BatchRequest batchRequest = createBaseRequest(cf, inputRecipientType);

        batchRequest.setReferenceRequestDate(null);

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(batchRequest)).verifyComplete();

        ArgumentCaptor<InternalCodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = payloadCaptor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertNull(payload.getReferenceRequestDate());
    }

    @Test
    void sendListToDlqQueue_shouldSendAllPayloads() {

        BatchRequest pfRequest = createBaseRequest(PF_CF, null);
        BatchRequest pgRequest = createBaseRequest(PG_CF, null);

        pfRequest.setCorrelationId("correlation-pf~001");
        pgRequest.setCorrelationId("correlation-pg~001");

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.sendListToDlqQueue(List.of(pfRequest, pgRequest))).verifyComplete();

        ArgumentCaptor<InternalCodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService, times(2)).pushToInputDlqQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        List<InternalCodeSqsDto> payloads = payloadCaptor.getAllValues();

        assertEquals(2, payloads.size());

        InternalCodeSqsDto pfPayload = payloads.stream().filter(payload -> "correlation-pf".equals(payload.getCorrelationId())).findFirst().orElseThrow();
        InternalCodeSqsDto pgPayload = payloads.stream().filter(payload -> "correlation-pg".equals(payload.getCorrelationId())).findFirst().orElseThrow();

        assertEquals(PF_CF, pfPayload.getTaxId());
        assertEquals(RecipientType.PF.name(), pfPayload.getRecipientType());
        assertEquals("DIGITAL", pfPayload.getDomicileType());
        assertEquals(CLIENT_ID, pfPayload.getPnNationalRegistriesCxId());

        assertEquals(PG_CF, pgPayload.getTaxId());
        assertEquals(RecipientType.PG.name(), pgPayload.getRecipientType());
        assertEquals("DIGITAL", pgPayload.getDomicileType());
        assertEquals(CLIENT_ID, pgPayload.getPnNationalRegistriesCxId());
    }

    @Test
    void recoveryBatchSendToSqs_whenNoRequests_shouldDoNothing() {

        when(batchRequestRepository.getBatchRequestToSend(anyMap(), eq(100))).thenReturn(Mono.just(Page.create(Collections.emptyList())));

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        verify(batchRequestRepository).getBatchRequestToSend(anyMap(), eq(100));
        verify(batchRequestRepository, never()).update(any());
        verifyNoInteractions(sqsService);
    }

    @Test
    void recoveryBatchSendToSqs_whenConditionalCheckFails_shouldSkipRequest() {

        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);

        when(batchRequestRepository.getBatchRequestToSend(anyMap(), eq(100))).thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        verify(batchRequestRepository, timeout(1000)).update(same(batchRequest));
        verify(sqsService, after(200).never()).pushToOutputQueue(any(), anyString());
        verify(sqsService, after(200).never()).pushToInputDlqQueue(any(), anyString());
    }

    @Test
    void recoveryBatchSendToSqs_shouldSendExpectedPayloadToOutputQueue() {

        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto expectedPayload = mock(CodeSqsDto.class);

        when(batchRequestRepository.getBatchRequestToSend(anyMap(), eq(100))).thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(expectedPayload);
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.just(SendMessageResponse.builder().messageId("recovered-message").build()));

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        ArgumentCaptor<CodeSqsDto> payloadCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);

        verify(sqsService, timeout(1000)).pushToOutputQueue(payloadCaptor.capture(), eq(CLIENT_ID));

        assertSame(expectedPayload, payloadCaptor.getValue());
        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getReservationId());
        assertNotNull(batchRequest.getLastReserved());

        verify(batchRequestRepository, timeout(1000).atLeast(2)).update(same(batchRequest));
    }

    private static Stream<Arguments> recipientTypeCases() {
        return Stream.of(
                Arguments.of(PF_CF, null, RecipientType.PF),
                Arguments.of(PG_CF, null, RecipientType.PG)
        );
    }

    private BatchRequest createOutputBatchRequest(String cf) {

        BatchRequest batchRequest = createBaseRequest(cf, null);

        batchRequest.setMessage(MESSAGE);
        batchRequest.setStatus("OK");
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());

        return batchRequest;
    }

    private BatchRequest createDlqBatchRequest(String cf, String recipientType, LocalDateTime referenceRequestDate) {

        BatchRequest batchRequest = createBaseRequest(cf, recipientType);

        batchRequest.setStatus(BatchStatus.ERROR.getValue());
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest.setReferenceRequestDate(referenceRequestDate);

        return batchRequest;
    }

    private BatchRequest createBaseRequest(String cf, String recipientType) {

        BatchRequest batchRequest = new BatchRequest();

        batchRequest.setCf(cf);
        batchRequest.setClientId(CLIENT_ID);
        batchRequest.setCorrelationId(CORRELATION_ID_WITH_SUFFIX);
        batchRequest.setRecipientType(recipientType);

        return batchRequest;
    }

    @Test
    void sendToDlqQueue_shouldUseCurrentPgRedriveContractAndSplitCorrelationId() {
        BatchRequest request = new BatchRequest();
        request.setCf("RSSMRA80A01H501U");
        request.setClientId("clientId");
        request.setCorrelationId("correlationId~2026-10-10T10:15:30");
        request.setRecipientType("PF");
        request.setReferenceRequestDate(LocalDateTime.of(2026, 10, 10, 12, 30));

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq("clientId")))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(request))
                .verifyComplete();

        verify(sqsService).pushToInputDlqQueue(
                argThat(message ->
                        "RSSMRA80A01H501U".equals(message.getTaxId())
                                && "clientId".equals(message.getPnNationalRegistriesCxId())
                                && "correlationId".equals(message.getCorrelationId())
                                && "PF".equals(message.getRecipientType())
                                && "DIGITAL".equals(message.getDomicileType())
                                && message.getReferenceRequestDate() != null
                ),
                eq("clientId")
        );
    }
}