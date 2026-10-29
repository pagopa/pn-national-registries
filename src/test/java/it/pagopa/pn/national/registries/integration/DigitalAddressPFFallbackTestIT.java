package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Scenari funzionali PF DIGITAL con fallback INAD verso il nuovo IniPEC: selezione PEC personale/professionale,
 * validità degli indirizzi, fallback, full flow IniPEC, esiti ND/NF/ER, metadata di errore e retry di polling.
 *
 * <p>Configurazione: fallback PF attivo, INAD massive disattivato, old IniPEC disattivato; viene usato il superset
 * compatibile delle property IniPEC necessarie agli scenari di batching e retry, senza valori in conflitto.</p>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.request-retry-after=0",
        "pn.national-registries.inipec.batch-request-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inipec.polling-error-retry-after=0",
        "pn.national-registries.inipec.polling-in-progress-retry-after=0"
})
public class DigitalAddressPFFallbackTestIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    /**
     * Verifica che un PF non trovato su INAD prosegua sul fallback al nuovo IniPEC.
     */
    @Test
    void PF_Digital_INAD_NotFound_InipecFallback() {
        String correlationId = "pf-digital-inad-not-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-not-found-fallback@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica che venga selezionata la PEC professionale quando quella personale è scaduta.
     */
    @Test
    void shouldTakeProfessionalWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-professional-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal,
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica il fallback a IniPEC quando la PEC personale INAD risulta scaduta e non utilizzabile.
     */
    @Test
    void shouldFallbackToInipecWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-after-expired-pending@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }


    /**
     * Verifica che una PEC personale valida trovata su INAD chiuda il flusso senza invocare IniPEC.
     */
    @Test
    void PF_Digital_INAD_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-digital-personal-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Verifica la gestione di PEC personale invalida e PEC professionale valida con fallback PF abilitato.
     */
    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-digital-invalid-personal-professional-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                inadAddress("invalid-personal", null),
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }


    /**
     * Verifica che un errore tecnico INAD non venga trattato come not found e non attivi il fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_TechnicalError_NoInipecFallback() {
        String correlationId = "pf-digital-technical-fallback-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }


    /**
     * Verifica end-to-end che una PEC personale INAD scaduta porti al fallback IniPEC e a un risultato valido.
     */
    @Test
    void PF_Digital_INAD_ExpiredPersonal_InipecValid_FullFlow() {
        String correlationId = "pf-digital-expired-personal-inipec-valid-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredPersonal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);

        String pollingId = "inipec-expired-pf-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> inipecRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> "INIPEC".equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
        String batchId = inipecRequest.get("batchId").s();
        pollingJob.polling();

        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setElencoPec(List.of(pecResponse(VALID_PF_CF, "inipec-after-expired@pec.it")));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        inipecRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> "INIPEC".equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertNotNull(inipecRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inipec-after-expired@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void completeInipecFinal(String correlationId, String address) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        String batchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, address))));
        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();
        request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(address, payload.getDigitalAddress().getFirst().getAddress());
    }

    private void assertInipecPendingRequest(String correlationId) {
        List<Map<String, AttributeValue>> storedRequests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, storedRequests.size());
        Map<String, AttributeValue> storedRequest = storedRequests.getFirst();
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

private static final String PF_PROFESSIONAL = "WDSTKB60E09L538U";
    private static final String PF_BUSINESS_PROFESSIONAL = "PRPMGG80A41H501W";
    private static final String PF_INVALID = "FLPCPT69A65Z336P";
    private static final String PF_ND = "DLGMRA73D41F839C";
    private static final String PF_NF = "PRMRGG74T16H501R";
    private static final String PF_MISSING = "VDADGI80A01F839E";
    private static final String PF_ER = "RSSSRG80A01F839I";
/**
     * Verifica end-to-end che IniPEC restituisca correttamente una PEC professionale per PF.
     */
    @Test
    void PF_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pf-inipec-professional-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, PF_PROFESSIONAL));

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional@pec.it")
        )));
        pollInipec(batchId, pollingId, 1);

        CodeSqsDto payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Verifica end-to-end che vengano mantenute la PEC impresa e tutte le PEC professionali valide restituite da IniPEC.
     */
    @Test
    void PF_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pf-inipec-business-professionals-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, PF_BUSINESS_PROFESSIONAL));

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it")
        )));
        pollInipec(batchId, pollingId, 1);

        CodeSqsDto payload = finalPayload(correlationId);
        assertEquals(3, payload.getDigitalAddress().size());
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * Verifica end-to-end che esiti IniPEC invalidi, ND/NF o senza codice fiscale producano un risultato finale vuoto.
     */
    @Test
    void PF_Digital_FullFlow_InipecInvalidNdNfAndMissingTaxIdProduceEmptyFinalResults() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("pf-inipec-invalid-" + UUID.randomUUID(), PF_INVALID);
        cases.put("pf-inipec-nd-" + UUID.randomUUID(), PF_ND);
        cases.put("pf-inipec-nf-" + UUID.randomUUID(), PF_NF);
        cases.put("pf-inipec-missing-" + UUID.randomUUID(), PF_MISSING);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, cases);

        Pec nd = pecResponse(PF_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        Pec nf = pecResponse(PF_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PF_INVALID, "not-an-email"), nd, nf
        )));
        pollInipec(batchId, pollingId, cases.size());

        for (String correlationId : cases.keySet()) {
            CodeSqsDto payload = finalPayload(correlationId);
            assertEquals("INIPEC", payload.getRegistry());
            assertNotNull(payload.getDigitalAddress());
            assertTrue(payload.getDigitalAddress().isEmpty());
        }
    }

    /**
     * Verifica end-to-end che lo stato ER di IniPEC generi retry e termini in errore al limite configurato.
     */
    @Test
    void PF_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pf-inipec-er-" + UUID.randomUUID();
        String firstPollingId = "polling-first-" + UUID.randomUUID();
        String firstBatchId = startInipecBatch(firstPollingId, Map.of(correlationId, PF_ER));
        Pec er = pecResponse(PF_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(firstPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(firstBatchId, firstPollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());

        String secondPollingId = "polling-second-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(secondPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        request = requestByRegistry(correlationId, "INIPEC");
        String secondBatchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(secondPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(secondBatchId, secondPollingId, 1);

        request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("ERROR", request.get("status").s());
        assertEquals("INIPEC#ERROR", request.get("registryStatus").s());
        assertNull(request.get("resultPayload"));
    }

    /**
     * Verifica end-to-end che un polling IniPEC in-progress programmi il retry previsto.
     */
    @Test
    void PF_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pf-inipec-in-progress-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, VALID_PF_CF));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(
                new it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException("List PEC in progress", 404, "Not Found", null, null, java.nio.charset.StandardCharsets.UTF_8, null)
        ));
        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    /**
     * Verifica end-to-end che i metadata di errore IniPEC vengano gestiti come retry di polling.
     */
    @Test
    void PF_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pf-inipec-metadata-error-" + UUID.randomUUID();
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, VALID_PF_CF));
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setCode("123");
        response.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));
        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    private String startInipecBatch(String pollingId, Map<String, String> correlationTaxIds) {
        for (Map.Entry<String, String> entry : correlationTaxIds.entrySet()) {
            when(inadClient.callEService(eq(entry.getValue()), eq(entry.getKey()))).thenReturn(Mono.error(inadNotFoundException()));
            enqueueGatewayRequest("PF", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
            assertEquals("NOT_WORKED", requestByRegistry(entry.getKey(), "INIPEC").get("status").s());
        }
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = null;
        for (String correlationId : correlationTaxIds.keySet()) {
            Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
            assertEquals("WORKING", request.get("status").s());
            if (batchId == null) {
                batchId = request.get("batchId").s();
            } else {
                assertEquals(batchId, request.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());
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

    private CodeSqsDto finalPayload(String correlationId) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        return readOutputMessage(request.get("resultPayload").s());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
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

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
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
}
