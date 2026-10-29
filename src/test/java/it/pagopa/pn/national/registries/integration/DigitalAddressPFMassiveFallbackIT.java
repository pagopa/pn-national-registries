package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Scenari funzionali: Copre il full flow PF con INAD massive e fallback al nuovo IniPEC per risultato trovato, non trovato ed errore ER con retry.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>{@code pn.national-registries.enable-inad-massive-api=true}</li>
 *   <li>{@code pn.national-registries.enable-pf-pec-fallback-flow=true}</li>
 *   <li>{@code pn.national-registries.enable-old-inipec-flow=false}</li>
 *   <li>{@code pn.national-registries.inad.max-batch-request-size=4}</li>
 *   <li>{@code pn.national-registries.inad.oldest-request-max-waiting-seconds=0}</li>
 *   <li>{@code pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0}</li>
 *   <li>{@code pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0}</li>
 *   <li>{@code pn.national-registries.inipec.max-batch-request-size=4}</li>
 *   <li>{@code pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0}</li>
 *   <li>{@code pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0}</li>
 *   <li>{@code pn.national-registries.inipec.batch-request-max-retry=3}</li>
 *   <li>{@code pn.national-registries.inipec.request-retry-after=60}</li>
 *   <li>{@code spring.task.scheduling.enabled=false}</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inad.max-batch-request-size=4",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.max-batch-request-size=4",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=3",
        "pn.national-registries.inipec.request-retry-after=60",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPFMassiveFallbackIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_PROFESSIONAL_ONLY = "RSSMRA80A01H501C";
    private static final String PF_INVALID_PERSONAL = "RSSMRA80A01H501E";
    private static final String PF_ALL_INVALID = "RSSMRA80A01H501F";

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private InipecPollingService inipecPollingService;

    /**
     * Verifica end-to-end INAD massive → fallback IniPEC con PEC trovata.
     */
    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecFound_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-found-");
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pecResponse(VALID_PF_CF, "pf-final@pec.it"))));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("pf-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Verifica end-to-end INAD massive → fallback IniPEC con esito non trovato.
     */
    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecNotFound_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-not-found-");
        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pec)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    /**
     * Verifica end-to-end che un esito ER di IniPEC dopo fallback da INAD massive programmi il retry della richiesta.
     */
    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecErSchedulesRequestRetry_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-er-");
        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pec)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", request.get("registryStatus").s());
        assertEquals("1", request.get("retryCount").n());
        assertNotNull(request.get("attemptAfter"));
        assertNull(request.get("resultPayload"));
    }

    private Flow createInipecFallback(String prefix) {
        String correlationId = prefix + UUID.randomUUID();
        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();
        assertEquals("NOT_WORKED", findRequest(correlationId, "INAD").get("status").s());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress inadBatchResponse = new ResponseRequestListDigitalAddress();
        inadBatchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(inadBatchResponse));
        inadBatchRequestJob.batchRequest();
        Map<String, AttributeValue> inadRequest = findRequest(correlationId, "INAD");
        String inadBatchId = inadRequest.get("batchId").s();
        pollingJob.polling();

        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(status));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(result));
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, inadBatchId, inadPollingId)).block();

        inadRequest = findRequest(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNull(inadRequest.get("resultPayload"));
        assertEquals("NOT_WORKED", findRequest(correlationId, "INIPEC").get("status").s());

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = findRequest(correlationId, "INIPEC");
        String inipecBatchId = inipecRequest.get("batchId").s();
        pollingJob.polling();
        return new Flow(correlationId, inipecBatchId, inipecPollingId);
    }

    /**
     * Simula il caso in cui tutte le request INAD sono già WORKED e i fallback INIPEC sono già stati creati,
     * ma l'update finale del batch a WORKED è fallito lasciandolo WORKING. Al replay non devono essere creati
     * nuovi fallback e il batch deve essere semplicemente chiuso a WORKED.
     */
    @Test
    void InadToInipec_AllRequestsWorked_BatchUpdateFailed_ReplayOnlyClosesBatch() {
        String correlationId1 = "inad-inipec-batch-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-inipec-batch-failure-2-" + UUID.randomUUID();
        InadFlow flow = startInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId1);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId2);
        assertEquals("WORKED", request(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", request(correlationId2, "INAD").get("status").s());
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());

        inadPollingService.handleMessage(payload).block();

        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKED", request(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", request(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Simula il caso in cui il processing si interrompe dopo aver completato solo una parte delle request INAD.
     * Al replay devono essere recuperate solo le request ancora WORKING: i fallback già creati non devono essere
     * duplicati, mentre deve essere creato il fallback solo per le request rimaste da processare.
     */
    @Test
    void InadToInipec_RequestUpdateFailed_ReplayProcessesOnlyWorkingRequests() {
        String correlationId1 = "inad-inipec-request-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-inipec-request-failure-2-" + UUID.randomUUID();
        InadFlow flow = startInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId1);
        assertEquals("WORKED", request(correlationId1, "INAD").get("status").s());
        assertEquals("WORKING", request(correlationId2, "INAD").get("status").s());
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertTrue(requestsByRegistry(correlationId2, "INIPEC").isEmpty());
        assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());

        inadPollingService.handleMessage(payload).block();

        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKED", request(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", request(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Verifica che correlationId, clientId e referenceRequestDate restino invariati dalla richiesta INAD alla richiesta INIPEC di fallback.
     */
    @Test
    void InadToInipec_FallbackPreservesRequestIdentityAndReferenceDate() {
        String correlationId = "fallback-identity-" + UUID.randomUUID();
        InadFlow flow = startInadFlow(correlationId, VALID_PF_CF);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));

        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 1)).block();

        Map<String, AttributeValue> source = requestsByRegistry(correlationId, "INAD").getFirst();
        Map<String, AttributeValue> fallback = requestsByRegistry(correlationId, "INIPEC").getFirst();
        assertEquals(source.get("correlationId").s(), fallback.get("correlationId").s());
        assertEquals(source.get("clientId").s(), fallback.get("clientId").s());
        assertEquals(source.get("referenceRequestDate").s(), fallback.get("referenceRequestDate").s());
        assertEquals(source.get("taxId").s(), fallback.get("taxId").s());
        assertEquals(source.get("recipientType").s(), fallback.get("recipientType").s());
    }

    /**
     * Verifica in un unico batch che la selezione PF avvenga prima del fallback: professionale valida usata quando necessaria e fallback solo se nessun candidato valido resta utilizzabile.
     */
    @Test
    void PF_Massive_FallbackOn_SelectsProfessionalBeforeStartingInipecFallback() {
        Map<String, String> cases = Map.of(
                "professional-only-" + UUID.randomUUID(), PF_PROFESSIONAL_ONLY,
                "invalid-personal-" + UUID.randomUUID(), PF_INVALID_PERSONAL,
                "all-invalid-" + UUID.randomUUID(), PF_ALL_INVALID
        );
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            enqueueGatewayRequest("PF", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
        }

        String pollingId = "inad-selection-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();
        String batchId = request(correlationForTaxId(cases, PF_PROFESSIONAL_ONLY), "INAD").get("batchId").s();
        pollingJob.polling();

        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status));

        ResponseRequestDigitalAddress professionalOnly = inadResponse(PF_PROFESSIONAL_ONLY, List.of(
                inadAddress("professional-only@pec.it", "avvocato")
        ));
        ResponseRequestDigitalAddress invalidPersonal = inadResponse(PF_INVALID_PERSONAL, List.of(
                inadAddress("invalid-personal", null),
                inadAddress("professional-valid@pec.it", "commercialista")
        ));
        ResponseRequestDigitalAddress allInvalid = inadResponse(PF_ALL_INVALID, List.of(
                inadAddress("invalid-personal", null),
                inadAddress("invalid-professional", "ingegnere")
        ));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(professionalOnly, invalidPersonal, allInvalid));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));

        inadPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(3)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        String professionalCorrelation = correlationForTaxId(cases, PF_PROFESSIONAL_ONLY);
        CodeSqsDto professionalPayload = readOutputMessage(request(professionalCorrelation, "INAD").get("resultPayload").s());
        assertEquals("professional-only@pec.it", professionalPayload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", professionalPayload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(requestsByRegistry(professionalCorrelation, "INIPEC").isEmpty());

        String invalidPersonalCorrelation = correlationForTaxId(cases, PF_INVALID_PERSONAL);
        CodeSqsDto professionalFallbackPayload = readOutputMessage(request(invalidPersonalCorrelation, "INAD").get("resultPayload").s());
        assertEquals("professional-valid@pec.it", professionalFallbackPayload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", professionalFallbackPayload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(requestsByRegistry(invalidPersonalCorrelation, "INIPEC").isEmpty());

        String allInvalidCorrelation = correlationForTaxId(cases, PF_ALL_INVALID);
        assertEquals("WORKED", request(allInvalidCorrelation, "INAD").get("status").s());
        assertNull(request(allInvalidCorrelation, "INAD").get("resultPayload"));
        assertEquals("NOT_WORKED", request(allInvalidCorrelation, "INIPEC").get("status").s());
    }

    private void addRequestToInadBatch(String batchId, String sourceCorrelationId, String newCorrelationId) {
        Map<String, AttributeValue> source = request(sourceCorrelationId, "INAD");
        Map<String, AttributeValue> newRequest = new HashMap<>(source);
        newRequest.put("correlationId", AttributeValue.builder().s(newCorrelationId).build());
        newRequest.put("batchId", AttributeValue.builder().s(batchId).build());
        newRequest.put("status", AttributeValue.builder().s("WORKING").build());
        newRequest.put("registryStatus", AttributeValue.builder().s("INAD#WORKING").build());
        newRequest.remove("resultPayload");
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_REQUESTS_TABLE).item(newRequest));
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("batchSize", AttributeValue.builder().n("2").build());
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE).item(batch));
    }

    private void markInadRequestAsWorkedAndCreateInipecFallback(String correlationId) {
        Map<String, AttributeValue> inadRequest = request(correlationId, "INAD");
        Map<String, AttributeValue> workedInadRequest = new HashMap<>(inadRequest);
        workedInadRequest.put("status", AttributeValue.builder().s("WORKED").build());
        workedInadRequest.put("registryStatus", AttributeValue.builder().s("INAD#WORKED").build());
        workedInadRequest.remove("resultPayload");
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_REQUESTS_TABLE).item(workedInadRequest));
        Map<String, AttributeValue> inipecRequest = new HashMap<>(inadRequest);
        String fallbackCreatedAt = Instant.parse(inadRequest.get("createdAt").s()).plusNanos(1).toString();
        inipecRequest.put("createdAt", AttributeValue.builder().s(fallbackCreatedAt).build());
        inipecRequest.put("attemptAfter", AttributeValue.builder().s(fallbackCreatedAt).build());
        inipecRequest.put("registry", AttributeValue.builder().s("INIPEC").build());
        inipecRequest.put("status", AttributeValue.builder().s("NOT_WORKED").build());
        inipecRequest.put("registryStatus", AttributeValue.builder().s("INIPEC#NOT_WORKED").build());
        inipecRequest.remove("batchId");
        inipecRequest.remove("resultPayload");
        inipecRequest.remove("updatedAt");
        inipecRequest.remove("retryCount");
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_REQUESTS_TABLE).item(inipecRequest));
    }

    private String correlationForTaxId(Map<String, String> cases, String taxId) {
        return cases.entrySet().stream().filter(e -> taxId.equals(e.getValue())).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private Map<String, AttributeValue> request(String correlationId, String registry) {
        return requestsByRegistry(correlationId, registry).getFirst();
    }

    private InadFlow startInadFlow(String correlationId, String taxId) {
        enqueueGatewayRequest("PF", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();
        String pollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(response));
        inadBatchRequestJob.batchRequest();
        Map<String, AttributeValue> request = requestsByRegistry(correlationId, "INAD").getFirst();
        String batchId = request.get("batchId").s();
        pollingJob.polling();
        return new InadFlow(batchId, pollingId);
    }

    private List<Map<String, AttributeValue>> requestsByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .toList();
    }

    private PnPollingEvent.Payload pollingPayload(GatewayDownstreamService registry, String batchId, String pollingId, int batchSize) {
        return PnPollingEvent.Payload.builder()
                .registry(registry)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(batchSize)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private ResponseStatusListDigitalAddress status(StatusProcessingRequest state) {
        ResponseStatusListDigitalAddress response = new ResponseStatusListDigitalAddress();
        response.setState(state);
        return response;
    }

    private record InadFlow(String batchId, String pollingId) {
    }


    private Map<String, AttributeValue> findRequest(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

    private PnPollingEvent.Payload pollingPayload(GatewayDownstreamService registry, String batchId, String pollingId) {
        return PnPollingEvent.Payload.builder()
                .registry(registry)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private IniPecPollingResponse inipecResponse(Pec pec) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        return response;
    }

    private Map<String, AttributeValue> findBatch(String batchId) {
        return dynamoDbClient.getItem(builder -> builder
                .tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)
                .key(Map.of(
                        "batchId", AttributeValue.builder().s(batchId).build(),
                        "sk", AttributeValue.builder().s("-").build()
                ))
        ).item();
    }

    private record Flow(String correlationId, String inipecBatchId, String inipecPollingId) {
    }
}
