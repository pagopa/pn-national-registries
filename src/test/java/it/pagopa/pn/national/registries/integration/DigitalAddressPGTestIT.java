package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetIpaPecRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import static org.mockito.Mockito.when;

/**
 * Scenari funzionali PG DIGITAL: ricerca IPA, fallback al nuovo IniPEC e successivo INAD, selezione degli indirizzi,
 * esiti ND/NF/ER, retry e verifica che il flag di fallback PF non modifichi il comportamento PG.
 *
 * <p>Configurazione: INAD massive disattivato e old IniPEC disattivato; viene applicato il superset compatibile
 * delle property di batching/polling IniPEC e del flag fallback PF, senza valori in conflitto.</p>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-polling-max-retry=3",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=24",
        "pn.national-registries.inipec.polling-error-retry-after=1",
        "pn.national-registries.inipec.polling-in-progress-retry-after=1",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.request-retry-after=0",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.enable-pf-pec-fallback-flow=true"
})
public class DigitalAddressPGTestIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Autowired
    private PollingJob pollingJob;


    /**
     * Verifica che la stessa risposta IPA produca la stessa PEC tramite endpoint diretto e tramite Gateway PG.
     */
    @Test
    void Ipa_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String taxId = VALID_PG_PIVA;
        String correlationId = "ipa-gateway-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(taxId), anyString())).thenReturn(Mono.just(ipaFoundResponse(taxId, "same-ipa@pec.it")));

        IPARequestBodyDto directRequest = convertToGetIpaPecRequest(buildGatewayRequest("ipa-direct-" + UUID.randomUUID(), taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL));
        IPAPecDto direct = webTestClient.post()
                .uri("/national-registries-private/ipa/pec")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(directRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(IPAPecDto.class)
                .returnResult().getResponseBody();
        assertNotNull(direct);

        enqueueGatewayRequest("PG", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();
        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        CodeSqsDto gateway = readOutputMessage(output.body());

        assertEquals(direct.getDomicilioDigitale(), gateway.getDigitalAddress().getFirst().getAddress());
        assertEquals("IPA", gateway.getRegistry());
    }


    /**
     * Verifica il caso base di recupero dell’indirizzo digitale PG tramite IPA.
     */
    @Test
    void PG_Digital_IPA() {
        String correlationId = "pg-digital-ipa-found-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "pg@pec.it")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("IPA", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getAddressType());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("pg@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    /**
     * Verifica che, con più risultati IPA, venga usato il risultato previsto dal servizio WS05.
     */
    @Test
    void PG_Digital_IPA_MultipleResults_UsesWS05() {
        String correlationId = "pg-digital-ipa-multiple-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaMultipleResponse()));
        when(ipaClient.callEServiceWS05(eq("cod-amm-1"),any())).thenReturn(Mono.just(ipaWs05Response("ws05@pec.it")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("IPA", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("ws05@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(ipaClient).callEServiceWS05(eq("cod-amm-1"),any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    /**
     * Verifica il fallback da IPA non trovato a IniPEC con PEC trovata.
     */
    @Test
    void PG_Digital_IPA_NotFound_InipecFound() {
        String correlationId = "pg-digital-ipa-not-found-inipec-found-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(VALID_PG_PIVA, "inipec@pec.it")
        )));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
        assertEquals(batchId, inipecRequest.get("batchId").s());
        assertNotNull(inipecRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inipec@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(1, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestId(any(IniPecBatchRequest.class));
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verifyNoInteractions(inadClient);
    }

    /**
     * Verifica il fallback a IniPEC quando IPA restituisce una PEC non valida.
     */
    @Test
    void PG_Digital_IPA_InvalidPec_InipecFound() {
        String correlationId = "pg-digital-ipa-invalid-inipec-found-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "invalid-pec")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(VALID_PG_PIVA, "inipec-after-invalid-ipa@pec.it")
        )));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertNotNull(inipecRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals("inipec-after-invalid-ipa@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(1, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verifyNoInteractions(inadClient);
    }

    /**
     * Verifica la catena IPA not found → IniPEC not found → INAD sincrono.
     */
    @Test
    void PG_Digital_IPA_NotFound_InipecNotFound_InadSync() {
        String correlationId = "pg-digital-full-sync-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithState(VALID_PG_PIVA, "ignored@pec.it", Pec.StatoImpresaEnum.NF)
        )));
        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "inad-sync@pec.it", null)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
        assertNull(inipecRequest.get("resultPayload"));

        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));
        assertNull(inadRequest.get("batchId"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inad-sync@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(2, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verify(inadClient).callEService(VALID_PG_PIVA, correlationId);
    }


    /**
     * Verifica che un errore tecnico IPA non attivi i fallback successivi.
     */
    @Test
    void PG_Digital_IPA_TechnicalError_DoesNotFallback() {
        String correlationId = "pg-digital-ipa-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("IPA unavailable");
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    private String submitInipecBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> request = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKING", request.get("status").s());
        assertEquals("INIPEC#WORKING", request.get("registryStatus").s());
        assertNotNull(request.get("batchId"));

        String batchId = request.get("batchId").s();
        assertEquals("REQUEST_SENT", findBatch(batchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());
        return batchId;
    }

    private void assertInipecPendingRequest(String correlationId, String taxId) {
        Map<String, AttributeValue> storedRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private Map<String, AttributeValue> findRequestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

    private Map<String, AttributeValue> findBatch(String batchId) {
        return dynamoDbClient.getItem(builder -> builder
                        .tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)
                        .key(Map.of(
                                "batchId", AttributeValue.builder().s(batchId).build(),
                                "sk", AttributeValue.builder().s("-").build()
                        )))
                .item();
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

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }

    private Pec pecResponseWithState(String taxId, String pecAddress, Pec.StatoImpresaEnum statoImpresa) {
        Pec pec = pecResponse(taxId, pecAddress);
        pec.setStatoImpresa(statoImpresa);
        return pec;
    }

    private WS23ResponseDto ipaMultipleResponse() {
        ResultDto result = new ResultDto();
        result.setNumItems(2);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS23Dto first = new DataWS23Dto();
        first.setCodAmm("cod-amm-1");
        first.setDesAmm("Ente 1");
        first.setTipo("PA");
        first.setDomicilioDigitale("first@pec.it");

        DataWS23Dto second = new DataWS23Dto();
        second.setCodAmm("cod-amm-2");
        second.setDesAmm("Ente 2");
        second.setTipo("PA");
        second.setDomicilioDigitale("second@pec.it");

        WS23ResponseDto response = new WS23ResponseDto();
        response.setResult(result);
        response.setData(List.of(first, second));
        return response;
    }

    private WS05ResponseDto ipaWs05Response(String email) {
        ResultDto result = new ResultDto();
        result.setNumItems(1);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS05Dto data = new DataWS05Dto();
        data.setCodAmm("cod-amm-1");
        data.setDesAmm("Ente 1");
        data.setMail1(email);

        WS05ResponseDto response = new WS05ResponseDto();
        response.setResult(result);
        response.setData(data);
        return response;
    }

private static final String PG_INVALID = "56815563368";
    private static final String PG_EMPTY = "28223913097";
    private static final String PG_ND = "39274018561";
    private static final String PG_NF = "83016259471";
    private static final String PG_MISSING = "12825810299";
    private static final String PG_EXPIRED = "00534280912";
    private static final String PG_VALID_EXPIRED = "03649090796";
    private static final String PG_ER = "17492068394";
/**
     * Verifica end-to-end che i not found IniPEC per PG proseguano su INAD sincrono applicando le regole di selezione PG.
     */
    @Test
    void PG_Digital_FullFlow_InipecNotFoundVariantsReachInadSyncAndApplyPgSelectionRules() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("pg-invalid-" + UUID.randomUUID(), PG_INVALID);
        cases.put("pg-empty-" + UUID.randomUUID(), PG_EMPTY);
        cases.put("pg-nd-" + UUID.randomUUID(), PG_ND);
        cases.put("pg-nf-" + UUID.randomUUID(), PG_NF);
        cases.put("pg-missing-" + UUID.randomUUID(), PG_MISSING);
        cases.put("pg-expired-" + UUID.randomUUID(), PG_EXPIRED);
        cases.put("pg-valid-expired-" + UUID.randomUUID(), PG_VALID_EXPIRED);
        String pollingId = "pg-inipec-polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, cases);

        Pec nd = pecResponse(PG_ND, "ignored@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        Pec nf = pecResponse(PG_NF, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        Pec expiredFallback = pecResponse(PG_EXPIRED, "ignored@pec.it");
        expiredFallback.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        Pec validExpiredFallback = pecResponse(PG_VALID_EXPIRED, "ignored@pec.it");
        validExpiredFallback.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PG_INVALID, "not-an-email"),
                pecResponse(PG_EMPTY, ""),
                nd,
                nf,
                expiredFallback,
                validExpiredFallback
        )));

        when(inadClient.callEService(eq(PG_INVALID), any())).thenReturn(Mono.just(inadResponse(PG_INVALID, "pg-invalid-fallback@pec.it", null)));
        when(inadClient.callEService(eq(PG_EMPTY), any())).thenReturn(Mono.just(inadResponse(PG_EMPTY, "pg-empty-fallback@pec.it", null)));
        when(inadClient.callEService(eq(PG_ND), any())).thenReturn(Mono.just(inadResponse(PG_ND, List.of(
                inadAddress("first@pec.it", null), inadAddress("second@pec.it", null)
        ))));
        when(inadClient.callEService(eq(PG_NF), any())).thenReturn(Mono.just(inadResponse(PG_NF, List.of(
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));
        when(inadClient.callEService(eq(PG_MISSING), any())).thenReturn(Mono.just(inadResponse(PG_MISSING, List.of(
                inadAddress("not-an-email", null)
        ))));
        ElementDigitalAddress expired = inadAddress("expired@pec.it", null);
        expired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(PG_EXPIRED), any())).thenReturn(Mono.just(inadResponse(PG_EXPIRED, List.of(expired))));
        ElementDigitalAddress anotherExpired = inadAddress("expired-second@pec.it", null);
        anotherExpired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(PG_VALID_EXPIRED), any())).thenReturn(Mono.just(inadResponse(PG_VALID_EXPIRED, List.of(
                inadAddress("valid@pec.it", null), anotherExpired
        ))));

        pollInipec(batchId, pollingId, cases.size());

        assertInadFinal(correlationFor(cases, PG_INVALID), "pg-invalid-fallback@pec.it");
        assertInadFinal(correlationFor(cases, PG_EMPTY), "pg-empty-fallback@pec.it");
        assertInadEmpty(correlationFor(cases, PG_ND));
        assertInadEmpty(correlationFor(cases, PG_NF));
        assertInadEmpty(correlationFor(cases, PG_MISSING));
        assertInadEmpty(correlationFor(cases, PG_EXPIRED));
        assertInadFinal(correlationFor(cases, PG_VALID_EXPIRED), "valid@pec.it");
    }

    /**
     * Verifica end-to-end che un esito ER IniPEC programmi il retry senza creare una richiesta INAD.
     */
    @Test
    void PG_Digital_FullFlow_InipecErSchedulesRequestRetryWithoutCreatingInad() {
        String correlationId = "pg-er-" + UUID.randomUUID();
        String pollingId = "pg-er-polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, PG_ER));
        Pec er = pecResponse(PG_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).stream().noneMatch(item -> "INAD".equals(item.get("registry").s())));
    }

    private String startInipecBatch(String pollingId, Map<String, String> cases) {
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            when(ipaClient.callEServiceWS23(eq(entry.getValue()), any())).thenReturn(Mono.just(ipaNotFoundResponse()));
            enqueueGatewayRequest("PG", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
            assertEquals("NOT_WORKED", requestByRegistry(entry.getKey(), "INIPEC").get("status").s());
        }
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = null;
        for (String correlationId : cases.keySet()) {
            Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
            assertEquals("WORKING", request.get("status").s());
            if (batchId == null) batchId = request.get("batchId").s();
            else assertEquals(batchId, request.get("batchId").s());
        }
        assertNotNull(batchId);
        pollingJob.polling();
        return batchId;
    }

    private void pollInipec(String batchId, String pollingId, int batchSize) {
        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(batchSize)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();
    }

    private String correlationFor(Map<String, String> cases, String taxId) {
        return cases.entrySet().stream().filter(entry -> taxId.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private void assertInadFinal(String correlationId, String expectedAddress) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", request.get("status").s());
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(expectedAddress, payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void assertInadEmpty(String correlationId) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", request.get("status").s());
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

/**
     * Verifica end-to-end che l’abilitazione del fallback PF non modifichi il comportamento del flusso PG.
     */
    @Test
    void PG_Digital_FullFlow_IsUnchangedWhenPfFallbackIsEnabled() {
        String correlationId = "pg-pf-fallback-enabled-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        Map<String, AttributeValue> pending = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", pending.get("status").s());

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = requestByRegistry(correlationId, "INIPEC");
        String batchId = inipecRequest.get("batchId").s();
        pollingJob.polling();

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "pg-pf-fallback-enabled@pec.it", null)));

        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNotNull(inadRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("pg-pf-fallback-enabled@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }
}
