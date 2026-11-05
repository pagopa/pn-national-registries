package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniPecBatchSqsServiceTest {

    private static final String CLIENT_ID = "clientId";
    private static final String MESSAGE = "message";
    private static final String PF_CF = "RSSMRA80A01H501U";
    private static final String PG_CF = "12345678901";
    private static final String CORRELATION_ID = "correlationId";
    private static final String CORRELATION_ID_WITH_SUFFIX = "correlationId~001";
    private static final String BATCH_REQUEST_PK_SEPARATOR = "~";
    private static final Integer TTL = 3600;

    @Mock
    private IniPecBatchRequestRepository batchRequestRepository;

    @Mock
    private SqsService sqsService;

    private NationalRegistriesConfig nationalRegistriesConfig;
    private IniPecBatchSqsService iniPecBatchSqsService;

    @BeforeEach
    void setUp() {
        nationalRegistriesConfig = mock(NationalRegistriesConfig.class, Answers.RETURNS_DEEP_STUBS);
        iniPecBatchSqsService = new IniPecBatchSqsService(batchRequestRepository, sqsService, nationalRegistriesConfig, BATCH_REQUEST_PK_SEPARATOR);
    }

    @Test
    void batchSendToSqs_emptyList_shouldCompleteWithoutInteractions() {
        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(Collections.emptyList())).verifyComplete();

        verifyNoInteractions(batchRequestRepository);
        verifyNoInteractions(sqsService);
    }

    @Test
    void batchSendToSqs_whenOutputQueueSendSucceeds_shouldBecomeSent() {
        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto codeSqsDto = mock(CodeSqsDto.class);

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(codeSqsDto);
        when(sqsService.pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getReservationId());
        assertNotNull(batchRequest.getLastReserved());
        assertNotNull(batchRequest.getTtl());

        verify(sqsService).pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID));
        verify(batchRequestRepository).update(same(batchRequest));
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
    }

    @Test
    void batchSendToSqs_whenOutputQueueSendFails_shouldRemainNotSent() {
        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto codeSqsDto = mock(CodeSqsDto.class);

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(codeSqsDto);
        when(sqsService.pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("Output queue error").build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());

        verify(sqsService).pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID));
        verify(batchRequestRepository, never()).update(any(BatchRequest.class));
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
    }

    @ParameterizedTest(name = "DLQ success: recipientType {2}")
    @MethodSource("recipientTypeCases")
    void batchSendToSqs_whenDlqSendSucceeds_shouldBecomeSentToDlq(String cf, String inputRecipientType, RecipientType expectedRecipientType) {
        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 10, 30);
        BatchRequest batchRequest = createDlqBatchRequest(cf, inputRecipientType, referenceRequestDate);

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getTtl());

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals(Date.from(referenceRequestDate.atZone(ZoneId.systemDefault()).toInstant()), payload.getReferenceRequestDate());

        verify(batchRequestRepository).update(same(batchRequest));
        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
    }

    @ParameterizedTest(name = "DLQ failure: recipientType {2}")
    @MethodSource("recipientTypeCases")
    void batchSendToSqs_whenDlqSendFails_shouldRemainNotSent(String cf, String inputRecipientType, RecipientType expectedRecipientType) {
        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 10, 30);
        BatchRequest batchRequest = createDlqBatchRequest(cf, inputRecipientType, referenceRequestDate);

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("DLQ error").build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).expectError(SqsException.class).verify();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());

        verify(batchRequestRepository, never()).update(any(BatchRequest.class));
        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
    }

    @Test
    void batchSendToSqs_whenReservationFails_shouldRemainNotSent() {
        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest))).verifyComplete();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());

        verify(sqsService, never()).pushToOutputQueue(any(), anyString());
        verify(sqsService, never()).pushToInputDlqQueue(any(), anyString());
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    void batchSendToSqs_mixedBatch_shouldKeepOnlySuccessfulItemSent() {
        BatchRequest reservationKo = createOutputBatchRequest(PF_CF);
        BatchRequest outputKo = createOutputBatchRequest(PF_CF);
        BatchRequest outputOk = createOutputBatchRequest(PF_CF);

        reservationKo.setCorrelationId("correlation-reservation-ko~001");
        outputKo.setCorrelationId("correlation-output-ko~001");
        outputOk.setCorrelationId("correlation-output-ok~001");

        CodeSqsDto outputKoPayload = mock(CodeSqsDto.class);
        CodeSqsDto outputOkPayload = mock(CodeSqsDto.class);

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(reservationKo))).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(outputKo))).thenReturn(Mono.just(outputKo));
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(outputOk))).thenReturn(Mono.just(outputOk));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(outputKoPayload, outputOkPayload);
        when(sqsService.pushToOutputQueue(same(outputKoPayload), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("Output KO").build()));
        when(sqsService.pushToOutputQueue(same(outputOkPayload), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(same(outputOk))).thenReturn(Mono.just(outputOk));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(reservationKo, outputKo, outputOk))).verifyComplete();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), reservationKo.getSendStatus());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), outputKo.getSendStatus());
        assertEquals(BatchSendStatus.SENT.getValue(), outputOk.getSendStatus());

        verify(batchRequestRepository, never()).update(same(reservationKo));
        verify(batchRequestRepository, never()).update(same(outputKo));
        verify(batchRequestRepository).update(same(outputOk));
    }

    @ParameterizedTest(name = "sendToDlqQueue success: recipientType {2}")
    @MethodSource("recipientTypeCases")
    void sendToDlqQueue_whenSendSucceeds_shouldBecomeSentToDlq(String cf, String inputRecipientType, RecipientType expectedRecipientType) {
        LocalDateTime referenceRequestDate = LocalDateTime.of(2026, 10, 12, 8, 15);
        BatchRequest batchRequest = createBaseRequest(cf, inputRecipientType);

        batchRequest.setReferenceRequestDate(referenceRequestDate);

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(batchRequest)).expectNext(batchRequest).verifyComplete();

        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertEquals("DIGITAL", payload.getDomicileType());
        assertEquals(CLIENT_ID, payload.getPnNationalRegistriesCxId());
        assertEquals(CORRELATION_ID, payload.getCorrelationId());
        assertEquals(Date.from(referenceRequestDate.atZone(ZoneId.systemDefault()).toInstant()), payload.getReferenceRequestDate());

        verify(batchRequestRepository).update(same(batchRequest));
    }

    @Test
    void sendToDlqQueue_whenSendFails_shouldRemainNotSent() {
        BatchRequest batchRequest = createBaseRequest(PF_CF, null);

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.error(SqsException.builder().message("DLQ error").build()));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(batchRequest)).expectError(SqsException.class).verify();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());

        verify(batchRequestRepository, never()).update(any(BatchRequest.class));
    }

    @ParameterizedTest(name = "sendToDlqQueue null reference date: {2}")
    @MethodSource("recipientTypeCases")
    void sendToDlqQueue_whenReferenceRequestDateIsNull_shouldStillSend(String cf, String inputRecipientType, RecipientType expectedRecipientType) {
        BatchRequest batchRequest = createBaseRequest(cf, inputRecipientType);

        batchRequest.setReferenceRequestDate(null);

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));

        StepVerifier.create(iniPecBatchSqsService.sendToDlqQueue(batchRequest)).expectNext(batchRequest).verifyComplete();

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CLIENT_ID));

        InternalCodeSqsDto payload = captor.getValue();

        assertEquals(cf, payload.getTaxId());
        assertEquals(expectedRecipientType.name(), payload.getRecipientType());
        assertNull(payload.getReferenceRequestDate());
        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), batchRequest.getSendStatus());
    }

    @Test
    void sendListToDlqQueue_shouldSendAllItemsAndMarkThemSentToDlq() {
        BatchRequest pfRequest = createBaseRequest(PF_CF, null);
        BatchRequest pgRequest = createBaseRequest(PG_CF, null);

        pfRequest.setCorrelationId("correlation-pf~001");
        pgRequest.setCorrelationId("correlation-pg~001");

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), eq(CLIENT_ID))).thenReturn(Mono.empty());
        when(batchRequestRepository.update(any(BatchRequest.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(iniPecBatchSqsService.sendListToDlqQueue(List.of(pfRequest, pgRequest))).verifyComplete();

        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), pfRequest.getSendStatus());
        assertEquals(BatchSendStatus.SENT_TO_DLQ.getValue(), pgRequest.getSendStatus());

        ArgumentCaptor<InternalCodeSqsDto> captor = ArgumentCaptor.forClass(InternalCodeSqsDto.class);

        verify(sqsService, times(2)).pushToInputDlqQueue(captor.capture(), eq(CLIENT_ID));

        List<InternalCodeSqsDto> payloads = captor.getAllValues();

        assertEquals(2, payloads.size());

        InternalCodeSqsDto pfPayload = payloads.stream().filter(payload -> "correlation-pf".equals(payload.getCorrelationId())).findFirst().orElseThrow();
        InternalCodeSqsDto pgPayload = payloads.stream().filter(payload -> "correlation-pg".equals(payload.getCorrelationId())).findFirst().orElseThrow();

        assertEquals(PF_CF, pfPayload.getTaxId());
        assertEquals(RecipientType.PF.name(), pfPayload.getRecipientType());
        assertEquals(PG_CF, pgPayload.getTaxId());
        assertEquals(RecipientType.PG.name(), pgPayload.getRecipientType());

        verify(batchRequestRepository, times(2)).update(any(BatchRequest.class));
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
    void recoveryBatchSendToSqs_whenOutputSendSucceeds_shouldBecomeSent() {
        BatchRequest batchRequest = createOutputBatchRequest(PF_CF);
        CodeSqsDto codeSqsDto = mock(CodeSqsDto.class);

        when(nationalRegistriesConfig.getInipec().getTtl()).thenReturn(TTL);
        when(batchRequestRepository.getBatchRequestToSend(anyMap(), eq(100))).thenReturn(Mono.just(Page.create(List.of(batchRequest))));
        when(batchRequestRepository.update(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest))).thenReturn(Mono.just(batchRequest));
        when(sqsService.toObject(MESSAGE, CodeSqsDto.class)).thenReturn(codeSqsDto);
        when(sqsService.pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID))).thenReturn(Mono.empty());

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        verify(sqsService, timeout(1000)).pushToOutputQueue(same(codeSqsDto), eq(CLIENT_ID));

        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
        assertNotEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest.getSendStatus());
        assertNotNull(batchRequest.getReservationId());
        assertNotNull(batchRequest.getLastReserved());
        assertNotNull(batchRequest.getTtl());

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
        batchRequest.setReferenceRequestDate(LocalDateTime.now());
        batchRequest.setRecipientType(recipientType);
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());

        return batchRequest;
    }
}