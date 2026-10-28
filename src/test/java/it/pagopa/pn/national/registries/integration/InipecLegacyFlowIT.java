package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "spring.task.scheduling.enabled=false"
})
class InipecLegacyFlowIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void LegacyInipec_FullPfHappyPath() {
        String correlationId = "legacy-pf-found-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        assertLegacyRequest(correlationId, "NOT_WORKED", "NO_BATCH_ID");

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> working = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", working.get("status").s());
        assertNotEquals("NO_BATCH_ID", working.get("batchId").s());
        String batchId = working.get("batchId").s();
        assertLegacyPolling(batchId, pollingId, "NOT_WORKED", 0, 0);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "legacy-pf@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", worked.get("status").s());
        assertEquals("INIPEC", worked.get("eservice").s());
        assertNotNull(worked.get("message"));
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("legacy-pf@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_PfNotFoundProducesEmptyFinalResult() {
        String correlationId = "legacy-pf-not-found-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", worked.get("status").s());
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    @Test
    void LegacyInipec_PgNotFoundFallsBackToInadSync() {
        String correlationId = "legacy-pg-inad-found-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PG_PIVA);
        String batchId = submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));
        when(inadClient.callEService(eq(VALID_PG_PIVA), startsWith(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "legacy-inad@pec.it", null)));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", worked.get("status").s());
        assertEquals("INAD", worked.get("eservice").s());
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("legacy-inad@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_PgInadNotFoundProducesEmptyResult() {
        String correlationId = "legacy-pg-inad-not-found-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PG_PIVA);
        String batchId = submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));
        when(inadClient.callEService(eq(VALID_PG_PIVA), startsWith(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", worked.get("status").s());
        assertEquals("INAD", worked.get("eservice").s());
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals("INAD", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_AllProfessionalPecsArePreserved() {
        String correlationId = "legacy-professionals-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(
                pecResponseWithProfessionals(VALID_PF_CF, null, "professional-one@pec.it", "professional-two@pec.it")
        )));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", worked.get("status").s());
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals(2, payload.getDigitalAddress().size());
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress())));
        assertTrue(payload.getDigitalAddress().stream().allMatch(address -> "PROFESSIONISTA".equals(address.getRecipient())));
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "legacy-mixed-validity-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(
                pecResponseWithProfessionals(VALID_PF_CF, "not-an-email", "professional@pec.it")
        )));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_NdIgnoresPecsAndProducesEmptyPfResult() {
        String correlationId = "legacy-nd-with-pec-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        Pec pec = pecResponseWithProfessionals(VALID_PF_CF, "business@pec.it", "professional@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> worked = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        CodeSqsDto payload = readOutputMessage(worked.get("message").s());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_BusinessAndProfessionalPecsAreBothPreserved() {
        String correlationId = "legacy-business-professional-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(
                pecResponseWithProfessionals(VALID_PF_CF, "business@pec.it", "professional@pec.it")
        )));

        digitalAddressBatchPollingService.batchPecPolling();

        CodeSqsDto payload = readOutputMessage(findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("message").s());
        assertEquals(2, payload.getDigitalAddress().size());
        assertEquals("business@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional@pec.it".equals(address.getAddress())));
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_AllInvalidPecsAreTreatedAsNotFoundForPf() {
        String correlationId = "legacy-invalid-pecs-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(
                pecResponseWithProfessionals(VALID_PF_CF, "invalid-business", "invalid-professional")
        )));

        digitalAddressBatchPollingService.batchPecPolling();

        CodeSqsDto payload = readOutputMessage(findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("message").s());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_MissingTaxIdInResponseIsTreatedAsNotFoundForPf() {
        String correlationId = "legacy-missing-cf-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse("RSSMRA80A01H501U", "other@pec.it"))));

        digitalAddressBatchPollingService.batchPecPolling();

        CodeSqsDto payload = readOutputMessage(findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("message").s());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_SubmitErrorIncrementsRetryWithoutClosingRequest() {
        String correlationId = "legacy-submit-retry-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("TAKEN_CHARGE", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNotEquals("NO_BATCH_ID", request.get("batchId").s());
    }

    @Test
    void LegacyInipec_PollingBadRequestMarksRequestAndPollingErrorImmediately() {
        String correlationId = "legacy-polling-bad-request-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(badRequest));

        digitalAddressBatchPollingService.batchPecPolling();

        awaitLegacyPolling(batchId, "ERROR", 1, 0);
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertNotNull(request.get("message"));
    }

    @Test
    void LegacyInipec_ErrorMetadataIsHandledAsPollingRetry() {
        String correlationId = "legacy-polling-metadata-error-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setDescription("provider metadata error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        digitalAddressBatchPollingService.batchPecPolling();

        awaitLegacyPolling(batchId, "WORKING", 1, 0);
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    @Test
    void LegacyInipec_ErIncrementsRequestRetryWithoutProducingResult() {
        String correlationId = "legacy-er-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("TAKEN_CHARGE", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNull(request.get("message"));
        assertLegacyPolling(batchId, pollingId, "WORKED", 0, 0);
    }

    @Test
    void LegacyInipec_PollingInProgressIncrementsInProgressRetry() {
        String correlationId = "legacy-in-progress-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);

        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        digitalAddressBatchPollingService.batchPecPolling();

        awaitLegacyPolling(batchId, "WORKING", 0, 1);
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    @Test
    void LegacyInipec_GenericPollingErrorIncrementsNormalRetry() {
        String correlationId = "legacy-polling-error-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        digitalAddressBatchPollingService.batchPecPolling();

        awaitLegacyPolling(batchId, "WORKING", 1, 0);
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    @Test
    void LegacyInipec_PgInadTechnicalErrorMarksLegacyRequestError() {
        String correlationId = "legacy-pg-inad-error-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PG_PIVA);
        submitLegacyBatch(correlationId, pollingId);

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pec)));
        when(inadClient.callEService(eq(VALID_PG_PIVA), startsWith(correlationId))).thenReturn(Mono.error(new IllegalStateException("INAD unavailable")));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("INAD", request.get("eservice").s());
        assertNull(request.get("message"));
    }

    private String submitLegacyBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", request.get("status").s());
        assertNotNull(request.get("batchId"));
        assertNotEquals("NO_BATCH_ID", request.get("batchId").s());
        return request.get("batchId").s();
    }

    private void assertLegacyRequest(String correlationId, String status, String batchId) {
        List<Map<String, AttributeValue>> requests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, requests.size());
        assertEquals(status, requests.getFirst().get("status").s());
        assertEquals(batchId, requests.getFirst().get("batchId").s());
    }

    private void assertLegacyPolling(String batchId, String pollingId, String status, int retry, int inProgressRetry) {
        Map<String, AttributeValue> polling = awaitLegacyPolling(batchId, status, retry, inProgressRetry);
        assertEquals(pollingId, polling.get("pollingId").s());
    }

    private Map<String, AttributeValue> awaitLegacyPolling(String batchId, String status, int retry, int inProgressRetry) {
        Map<String, AttributeValue> polling = null;
        for (int i = 0; i < 50; i++) {
            polling = findLegacyPolling(batchId);
            if (status.equals(polling.get("status").s())
                    && String.valueOf(retry).equals(polling.get("retry").n())
                    && String.valueOf(inProgressRetry).equals(polling.get("inProgressRetry").n())) {
                return polling;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        fail("Unexpected legacy polling state for batchId " + batchId
                + ": status=" + polling.get("status").s()
                + ", retry=" + polling.get("retry").n()
                + ", inProgressRetry=" + polling.get("inProgressRetry").n());
        return polling;
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

    private Pec pecResponseWithProfessionals(String taxId, String pecImpresa, String... professionalPecs) {
        Map<String, Object> value = new HashMap<>();
        value.put("cf", taxId);
        if (pecImpresa != null) {
            value.put("pecImpresa", pecImpresa);
        }
        value.put("pecProfessionista", List.of(professionalPecs).stream().map(pec -> Map.of("pec", pec)).toList());
        return objectMapper.convertValue(value, Pec.class);
    }

    private IniPecPollingResponse iniPecPollingResponse(Pec pec) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        return response;
    }
}