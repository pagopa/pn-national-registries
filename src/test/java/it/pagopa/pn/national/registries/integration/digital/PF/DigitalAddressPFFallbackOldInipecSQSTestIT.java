package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.IniPecBatchSqsService;
import it.pagopa.pn.national.registries.service.SqsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=1",
        "pn.national-registries.inipec.batch.polling.max-retry=1",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=1",
        "pn.national-registries.inipec.batch.polling.recovery.after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=1",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=1",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.batch-request-recovery-after=0"
})
class DigitalAddressPFFallbackOldInipecSQSTestIT extends AbstractNationalRegistriesComponentIT {

    @MockitoSpyBean
    private SqsService sqsService;

    @Autowired
    private IniPecBatchSqsService iniPecBatchSqsService;

    @Test
    void batchRequest_dlqOk_shouldSetErrorAndSentToDlq() {
        String correlationId = "batch-request-dlq-ok-" + UUID.randomUUID();

        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);

        PnNationalRegistriesException badRequest = badRequestException();

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("ERROR", request.get("status").s());
        assertEquals("SENT_TO_DLQ", request.get("sendStatus").s());
        assertEquals("1", request.get("retry").n());
        assertNotNull(request.get("ttl"));
        assertTrue(findPollingByBatchId(request.get("batchId").s()).isEmpty());

        Message dlqMessage = receiveSingleMessage(INPUT_DLQ_QUEUE);

        assertNotNull(dlqMessage);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void batchRequest_dlqKo_shouldRecoverAndSetSentToDlq() {
        String correlationId = "batch-request-dlq-ko-" + UUID.randomUUID();

        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);

        PnNationalRegistriesException badRequest = badRequestException();

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        doReturn(Mono.error(new RuntimeException("DLQ unavailable")))
                .when(sqsService)
                .pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("ERROR", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());
        assertEquals("1", request.get("retry").n());
        assertNull(request.get("ttl"));
        assertTrue(findPollingByBatchId(request.get("batchId").s()).isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        doCallRealMethod()
                .when(sqsService)
                .pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        Map<String, AttributeValue> recoveredRequest = awaitSendStatus(correlationId, "SENT_TO_DLQ");

        assertEquals("ERROR", recoveredRequest.get("status").s());
        assertEquals("SENT_TO_DLQ", recoveredRequest.get("sendStatus").s());
        assertEquals("1", recoveredRequest.get("retry").n());
        assertNotNull(recoveredRequest.get("ttl"));
        assertTrue(findPollingByBatchId(recoveredRequest.get("batchId").s()).isEmpty());

        Message recoveredDlqMessage = receiveSingleMessage(INPUT_DLQ_QUEUE);

        assertNotNull(recoveredDlqMessage);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService, times(2)).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void polling_dlqOk_shouldSetPollingErrorAndRequestSentToDlq() {
        String correlationId = "polling-dlq-ok-" + UUID.randomUUID();
        String pollingId = "polling-dlq-ok-" + UUID.randomUUID();

        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);

        PnNationalRegistriesException badRequest = badRequestException();

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(badRequest));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("ERROR", polling.get("status").s());
        assertEquals("ERROR", request.get("status").s());
        assertEquals("SENT_TO_DLQ", request.get("sendStatus").s());
        assertNotNull(request.get("ttl"));

        Message dlqMessage = receiveSingleMessage(INPUT_DLQ_QUEUE);

        assertNotNull(dlqMessage);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void polling_dlqKo_shouldRecoverAndSetSentToDlq() {
        String correlationId = "polling-dlq-ko-" + UUID.randomUUID();
        String pollingId = "polling-dlq-ko-" + UUID.randomUUID();

        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);

        PnNationalRegistriesException badRequest = badRequestException();

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(badRequest));

        doReturn(Mono.error(new RuntimeException("DLQ unavailable")))
                .when(sqsService)
                .pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("ERROR", polling.get("status").s());
        assertEquals("ERROR", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());
        assertNull(request.get("ttl"));

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        doCallRealMethod()
                .when(sqsService)
                .pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        Map<String, AttributeValue> recoveredRequest = awaitSendStatus(correlationId, "SENT_TO_DLQ");
        Map<String, AttributeValue> recoveredPolling = findLegacyPolling(batchId);

        assertEquals("ERROR", recoveredPolling.get("status").s());
        assertEquals("ERROR", recoveredRequest.get("status").s());
        assertEquals("SENT_TO_DLQ", recoveredRequest.get("sendStatus").s());
        assertNotNull(recoveredRequest.get("ttl"));

        Message recoveredDlqMessage = receiveSingleMessage(INPUT_DLQ_QUEUE);

        assertNotNull(recoveredDlqMessage);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        verify(sqsService, times(2)).pushToInputDlqQueue(any(InternalCodeSqsDto.class), any());
        verify(sqsService, never()).pushToOutputQueue(any(), any());
    }

    @Test
    void directOutput_ok_shouldSendMessageAndCreateNoBatchRequest() {
        String correlationId = "direct-output-ok-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                VALID_PF_CF,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);

        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());

        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(sqsService).pushToOutputQueue(any(CodeSqsDto.class), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void directOutput_ko_shouldPropagateErrorAndCreateNoBatchRequest() {
        String correlationId = "direct-output-ko-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        doReturn(Mono.error(new RuntimeException("Output queue unavailable")))
                .when(sqsService)
                .pushToOutputQueue(any(CodeSqsDto.class), any());

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                VALID_PF_CF,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertNotNull(thrown);

        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(sqsService).pushToOutputQueue(any(CodeSqsDto.class), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void polling_outputOk_shouldSetPollingWorkedAndRequestSent() {
        String correlationId = "polling-output-ok-" + UUID.randomUUID();
        String pollingId = "polling-output-ok-" + UUID.randomUUID();

        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(
                        iniPecPollingResponse(
                                pecResponse(VALID_PF_CF, "legacy-output-ok@pec.it")
                        )
                ));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("WORKED", polling.get("status").s());
        assertEquals("WORKED", request.get("status").s());
        assertEquals("SENT", request.get("sendStatus").s());
        assertNotNull(request.get("ttl"));
        assertNotNull(request.get("message"));

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);

        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("legacy-output-ok@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(sqsService).pushToOutputQueue(any(CodeSqsDto.class), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    @Test
    void polling_outputKo_shouldRecoverAndSetSent() {
        String correlationId = "polling-output-ko-" + UUID.randomUUID();
        String pollingId = "polling-output-ko-" + UUID.randomUUID();

        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(
                        iniPecPollingResponse(
                                pecResponse(VALID_PF_CF, "legacy-output-ko@pec.it")
                        )
                ));

        doReturn(Mono.error(new RuntimeException("Output queue unavailable")))
                .when(sqsService)
                .pushToOutputQueue(any(CodeSqsDto.class), any());

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("WORKED", polling.get("status").s());
        assertEquals("WORKED", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());
        assertNull(request.get("ttl"));
        assertNotNull(request.get("message"));

        CodeSqsDto storedPayload = readOutputMessage(request.get("message").s());

        assertEquals(correlationId, storedPayload.getCorrelationId());
        assertEquals("INIPEC", storedPayload.getRegistry());
        assertEquals("legacy-output-ko@pec.it", storedPayload.getDigitalAddress().getFirst().getAddress());

        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(sqsService).pushToOutputQueue(any(CodeSqsDto.class), any());

        doCallRealMethod()
                .when(sqsService)
                .pushToOutputQueue(any(CodeSqsDto.class), any());

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        Map<String, AttributeValue> recoveredRequest = awaitSendStatus(correlationId, "SENT");
        Map<String, AttributeValue> recoveredPolling = findLegacyPolling(batchId);

        assertEquals("WORKED", recoveredPolling.get("status").s());
        assertEquals("WORKED", recoveredRequest.get("status").s());
        assertEquals("SENT", recoveredRequest.get("sendStatus").s());
        assertNotNull(recoveredRequest.get("ttl"));
        assertNotNull(recoveredRequest.get("message"));

        Message recoveredOutputMessage = receiveSingleMessage(OUTPUT_QUEUE);

        assertNotNull(recoveredOutputMessage);

        CodeSqsDto recoveredPayload = readOutputMessage(recoveredOutputMessage.body());

        assertEquals(correlationId, recoveredPayload.getCorrelationId());
        assertEquals("INIPEC", recoveredPayload.getRegistry());
        assertEquals("legacy-output-ko@pec.it", recoveredPayload.getDigitalAddress().getFirst().getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(sqsService, times(2)).pushToOutputQueue(any(CodeSqsDto.class), any());
        verify(sqsService, never()).pushToInputDlqQueue(any(), any());
    }

    private PnNationalRegistriesException badRequestException() {
        return new PnNationalRegistriesException(
                "bad request",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                StandardCharsets.UTF_8,
                null
        );
    }

    private Map<String, AttributeValue> findBatchRequest(String correlationId) {
        List<Map<String, AttributeValue>> requests = findBatchRequestsByCorrelationPrefix(correlationId);

        assertEquals(1, requests.size());

        return requests.getFirst();
    }

    private String submitLegacyBatchFromGateway(String correlationId, String pollingId) {
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class)))
                .thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequest(correlationId);

        assertEquals("WORKING", request.get("status").s());
        assertNull(request.get("ttl"));

        String batchId = request.get("batchId").s();

        assertFalse(findPollingByBatchId(batchId).isEmpty());

        return batchId;
    }

    private Map<String, AttributeValue> findLegacyPolling(String batchId) {
        return findPollingByBatchId(batchId)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private List<Map<String, AttributeValue>> findPollingByBatchId(String batchId) {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(BATCH_POLLING_TABLE)
                                .filterExpression("batchId = :batchId")
                                .expressionAttributeValues(
                                        Map.of(
                                                ":batchId",
                                                AttributeValue.builder()
                                                        .s(batchId)
                                                        .build()
                                        )
                                )
                                .build()
                )
                .items();
    }

    private Map<String, AttributeValue> awaitSendStatus(String correlationId, String expectedSendStatus) {
        Map<String, AttributeValue> request = null;

        for (int i = 0; i < 100; i++) {
            request = findBatchRequest(correlationId);

            if (request.get("sendStatus") != null
                    && expectedSendStatus.equals(request.get("sendStatus").s())) {
                return request;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        fail("sendStatus non aggiornato a " + expectedSendStatus + " per correlationId " + correlationId);

        return request;
    }
}