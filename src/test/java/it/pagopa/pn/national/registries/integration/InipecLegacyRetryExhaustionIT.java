package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.inipec.batch-request-max-retry=1",
        "pn.national-registries.inipec.batch.polling.max-retry=2",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=2",
        "pn.national-registries.inipec.batch.polling.recovery.after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0"
})
class InipecLegacyRetryExhaustionIT extends AbstractNationalRegistriesComponentIT {

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
}