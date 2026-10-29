package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import java.nio.charset.StandardCharsets;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.service.IniPecBatchSqsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import software.amazon.awssdk.services.sqs.model.Message;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scenari funzionali PF DIGITAL con fallback verso old IniPEC: fallback da INAD, submit e polling legacy,
 * esaurimento retry, bad request, ER, in-progress, consegna dell'output e recovery del sendStatus.
 *
 * <p>Configurazione: old IniPEC e fallback PF attivi, INAD massive disattivato; viene usato il superset compatibile
 * delle property legacy per retry, recovery e polling immediato, senza valori in conflitto.</p>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=1",
        "pn.national-registries.inipec.batch.polling.max-retry=2",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=2",
        "pn.national-registries.inipec.batch.polling.recovery.after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.batch-request-recovery-after=0"
})
public class DigitalAddressPFFallbackOldInipecTestIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Verifica che un PF non trovato su INAD prosegua sul fallback legacy IniPEC.
     */
    @Test
    void PF_Digital_INAD_NotFound_OldInipecFallback() {
        String correlationId = "pf-old-inipec-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        Map<String, AttributeValue> legacyRequest = legacyRequests.getFirst();
        assertTrue(legacyRequest.get("correlationId").s().startsWith(correlationId + "~"));
        assertEquals(VALID_PF_CF, legacyRequest.get("cf").s());
        assertEquals("PF", legacyRequest.get("recipientType").s());
        assertEquals("NOT_WORKED", legacyRequest.get("status").s());
        String pollingId = "pf-old-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", legacyRequest.get("status").s());
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "pf-old-final@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", legacyRequest.get("status").s());
        CodeSqsDto payload = readOutputMessage(legacyRequest.get("message").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("pf-old-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(ipaClient, never()).callEServiceWS23(any(), any());
        verifyNoInteractions(anprClient);
    }

/**
     * Verifica che l’errore di submit al limite dei retry chiuda la richiesta legacy in errore.
     */
    @Test
    void LegacyInipec_SubmitErrorExhaustsRequestRetryAndMarksError() {
        String correlationId = "legacy-submit-error-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNotEquals("NO_BATCH_ID", request.get("batchId").s());
    }

    /**
     * Verifica che un bad request in submit chiuda immediatamente la richiesta legacy in errore.
     */
    @Test
    void LegacyInipec_SubmitBadRequestMarksRequestErrorImmediately() {
        String correlationId = "legacy-submit-bad-request-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
    }

    /**
     * Verifica che lo stato ER al limite dei retry chiuda la richiesta legacy in errore.
     */
    @Test
    void LegacyInipec_ErAtRequestRetryLimitMarksRequestError() {
        String correlationId = "legacy-er-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNull(request.get("message"));
    }

    /**
     * Verifica che l’esaurimento dei retry per errore polling chiuda in errore polling e richiesta.
     */
    @Test
    void LegacyInipec_PollingErrorExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-polling-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(new IllegalStateException("polling unavailable")));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("retry").n());

        recoverLegacyPolling(batchId);

        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("retry").n());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertNotNull(request.get("message"));
    }

    /**
     * Verifica che l’esaurimento dei retry in-progress chiuda in errore polling e richiesta.
     */
    @Test
    void LegacyInipec_PollingInProgressExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-in-progress-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);

        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("inProgressRetry").n());

        recoverLegacyPolling(batchId);

        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("inProgressRetry").n());

        assertEquals("ERROR", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    private String submitLegacyBatch(String correlationId, String pollingId) {
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", request.get("status").s());
        return request.get("batchId").s();
    }

    private void recoverLegacyPolling(String batchId) {
        digitalAddressBatchPollingService.recoveryBatchPolling();

        for (int i = 0; i < 50; i++) {
            if ("NOT_WORKED".equals(findLegacyPolling(batchId).get("status").s())) {
                return;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        fail("Legacy polling not recovered for batchId " + batchId);
    }

    private Map<String, AttributeValue> findLegacyPolling(String batchId) {
        return dynamoDbClient.scan(ScanRequest.builder()
                        .tableName(BATCH_POLLING_TABLE)
                        .filterExpression("batchId = :batchId")
                        .expressionAttributeValues(Map.of(":batchId", AttributeValue.builder().s(batchId).build()))
                        .build())
                .items()
                .stream()
                .findFirst()
                .orElseThrow();
    }

@Autowired
    private IniPecBatchSqsService iniPecBatchSqsService;

    @MockitoSpyBean
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;

    /**
     * Verifica che un risultato legacy venga realmente pubblicato sulla output queue e che la richiesta venga marcata SENT.
     */
    @Test
    void LegacyInipec_OutputIsReallyConsumedAndSendStatusBecomesSent() {
        String correlationId = "legacy-output-sent-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecResponse(VALID_PF_CF, "legacy-output@pec.it")));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));
        digitalAddressBatchPollingService.batchPecPolling();

        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        CodeSqsDto payload = readOutputMessage(output.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("legacy-output@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("SENT", request.get("sendStatus").s());
    }

    /**
     * Verifica che, se l'output è stato inviato ma il salvataggio di sendStatus=SENT fallisce, il recovery riprenda la richiesta e completi lo stato di consegna.
     */
    @Test
    void LegacyInipec_OutputSentButSendStatusPersistenceFails_RecoveryCompletesDeliveryState() {
        String correlationId = "legacy-output-recovery-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        AtomicBoolean failSentUpdateOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            BatchRequest request = invocation.getArgument(0);
            if (BatchSendStatus.SENT.getValue().equals(request.getSendStatus()) && failSentUpdateOnce.getAndSet(false)) {
                return Mono.error(new IllegalStateException("simulated sendStatus persistence failure"));
            }
            return invocation.callRealMethod();
        }).when(iniPecBatchRequestRepository).update(any(BatchRequest.class));

        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecResponse(VALID_PF_CF, "legacy-recovery@pec.it")));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));
        digitalAddressBatchPollingService.batchPecPolling();

        Message firstOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(firstOutput);
        assertEquals(correlationId, readOutputMessage(firstOutput.body()).getCorrelationId());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());

        iniPecBatchSqsService.recoveryBatchSendToSqs();
        request = awaitSendStatus(correlationId, "SENT");
        assertEquals("SENT", request.get("sendStatus").s());

        Message recoveredOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(recoveredOutput);
        assertEquals(correlationId, readOutputMessage(recoveredOutput.body()).getCorrelationId());
    }

    private Map<String, AttributeValue> awaitSendStatus(String correlationId, String expected) {
        Map<String, AttributeValue> request = null;
        for (int i = 0; i < 100; i++) {
            request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
            if (request.get("sendStatus") != null && expected.equals(request.get("sendStatus").s())) {
                return request;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
        fail("sendStatus non aggiornato a " + expected + " per correlationId " + correlationId);
        return request;
    }
}
