package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL
 *
 * Configurazione:
 *  - fallback PF disattivo
 *  - INAD massivo attivo
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inad.max-batch-request-size=9",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=60",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1"
})
public class DigitalAddressPFMassiveNoFallbackIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_FOUND = "JSMKRV95P54H501D";
    private static final String PF_NOT_FOUND = "XLPJSM93B21H501O";
    private static final String PF_PROFESSIONAL_ONLY = "BSMGPR92R62F205X";
    private static final String PF_PERSONAL_AND_PROFESSIONAL = "KRSJSM88S03H501A";
    private static final String PF_INVALID_PERSONAL = "KPRSMP91H12F205O";
    private static final String PF_MULTIPLE_PROFESSIONALS = "RSSMSM85E15H501L";
    private static final String PF_EMPTY = "VRDLSM78B02F839R";
    private static final String PF_EXPIRED_PERSONAL_AND_PROFESSIONAL = "TSDSND90R10A952M";
    private static final String PF_EXPIRED_PERSONAL = "ZFPFYC82R41L723C";

    private static final List<String> VALID_FILLER_CFS = List.of(
            "KRSJSM88S03H501A",
            "JSMKRV95P54H501D",
            "KPRSMP91H12F205O",
            "VRDMRA77E66I700E",
            "NRELCU80E53H501M",
            "NREMRA09A27F205P",
            "BSMGPR92R62F205X",
            "DCNMRM85M49H501S",
            "XLPJSM93B21H501O"
    );

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    /**
     * Inad pec personale valida.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalFound() {
        String correlationId = "pf-massive-found-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, "pf@pec.it", null));
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad NotFound: il CF richiesto non compare nel risultato massivo.
     */
    @Test
    void PF_Digital_INAD_Massive_NotFound_NoFallback() {
        String correlationId = "pf-massive-not-found-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_NOT_FOUND, null);
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad risposta con lista PEC vuota.
     */
    @Test
    void PF_Digital_INAD_Massive_EmptyList_NoFallback() {
        String correlationId = "pf-massive-empty-list-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_EMPTY, inadResponse(PF_EMPTY, List.of()));
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad pec personale scaduta.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalExpired_NoFallback() {
        String correlationId = "pf-massive-expired-personal-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_EXPIRED_PERSONAL, inadResponse(PF_EXPIRED_PERSONAL, List.of(expiredPersonal)));
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad pec personale senza usageInfo.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalWithoutUsageInfo_IsValid() {
        String correlationId = "pf-massive-personal-no-usage-info-" + UUID.randomUUID();
        ElementDigitalAddress personal = new ElementDigitalAddress();
        personal.setDigitalAddress("personal-no-usage@pec.it");
        personal.setPracticedProfession(null);
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad pec personale senza data fine validita.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-massive-personal-no-end-validity-" + UUID.randomUUID();
        ElementDigitalAddress personal = inadAddress("personal-no-end@pec.it", null);
        personal.getUsageInfo().setDateEndValidity(null);
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Con piu PEC personali viene ignorata quella scaduta e selezionata la prima valida.
     */
    @Test
    void PF_Digital_INAD_Massive_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-massive-first-personal-invalid-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("first-expired@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress result = inadResponse(PF_FOUND, List.of(expiredPersonal, inadAddress("second-valid@pec.it", null)));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_FOUND, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale presente e professionale presente: viene preferita la personale.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInsteadOfProfessional_NoFallback() {
        String correlationId = "pf-massive-personal-preferred-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PERSONAL_AND_PROFESSIONAL, List.of(inadAddress("personal@pec.it", null), inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale assente e professionale presente con fallback disabilitato.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalNotFound_ProfessionalFound_NoFallback() {
        String correlationId = "pf-massive-professional-only-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PROFESSIONAL_ONLY, List.of(inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_PROFESSIONAL_ONLY, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad personale invalida e professionale valida con fallback disabilitato.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInvalid_ProfessionalValid_NoFallback() {
        String correlationId = "pf-massive-invalid-personal-professional-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_INVALID_PERSONAL, List.of(inadAddress("2026-01-01", null), inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_INVALID_PERSONAL, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad personale assente e piu PEC professionali valide con fallback disabilitato.
     */
    @Test
    void PF_Digital_INAD_Massive_MultipleProfessionals_NoFallback() {
        String correlationId = "pf-massive-multiple-professionals-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_MULTIPLE_PROFESSIONALS, List.of(inadAddress("prof1@pec.it", "INGEGNERE"), inadAddress("prof2@pec.it", "ARCHITETTO")));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_MULTIPLE_PROFESSIONALS, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Errore tecnico INAD massivo durante il recupero del risultato.
     */
    @Test
    void PF_Digital_INAD_Massive_Error() {
        List<TestRequest> requests = createNineRequests("pf-error");
        enqueueMassiveRequests(requests);
        String pollingId = "polling-error-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.error(new RuntimeException("INAD unavailable")));
        poll(batchId, pollingId, 9);
        NationalRegistriesBatch batch = getBatch(batchId);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(1, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId());
            assertEquals("WORKING", stored.get("status").s());
            assertNull(stored.get("resultPayload"));
        }
    }

    /**
     * Inad pec personale scaduta e professionale valida con fallback disabilitato.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalExpired_ProfessionalValid_NoFallback() {
        String correlationId = "pf-massive-expired-personal-professional-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress result = inadResponse(PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, List.of(expiredPersonal, inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * Inad solo professionale scaduta con fallback disabilitato.
     */
    @Test
    void PF_Digital_INAD_Massive_OnlyProfessionalExpired_NoFallback() {
        String correlationId = "pf-massive-only-professional-expired-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PF_PROFESSIONAL_ONLY, inadResponse(PF_PROFESSIONAL_ONLY, List.of(expiredProfessional)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD restituisce più risultati per lo stesso codice fiscale,
     * (preso il primo risultato restituito)
     */
    @Test
    void PF_Digital_INAD_Massive_DuplicateTaxIdKeepsFirstProviderResult() {
        List<TestRequest> requests = createNineRequests("duplicate-taxid");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-duplicate-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        List<ResponseRequestDigitalAddress> providerResults =
                new ArrayList<>();

        providerResults.add(
                inadResponse(
                        requests.getFirst().taxId(),
                        "first@pec.it",
                        null
                )
        );
        providerResults.add(
                inadResponse(
                        requests.getFirst().taxId(),
                        "second@pec.it",
                        null
                )
        );

        for (int i = 1; i < requests.size(); i++) {
            providerResults.add(
                    inadResponse(
                            requests.get(i).taxId(),
                            "duplicate-filler-" + i + "@pec.it",
                            null
                    )
            );
        }

        ResponseListDigitalAddress result =
                new ResponseListDigitalAddress();
        result.setList(providerResults);

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(result));

        poll(batchId, pollingId, 9);

        Map<String, AttributeValue> stored =
                request(requests.getFirst().correlationId());

        GatewayResponse payload =
                readNewOutputMessage(stored.get("resultPayload").s());

        assertEquals(
                "first@pec.it",
                payload.getPayload().getDigitalAddress().getFirst().getAddress()
        );
        assertFalse(
                stored.get("resultPayload").s().contains("second@pec.it")
        );
    }


    /**
     * Simula il caso in cui il provider abbia accettato il batch ma la persistenza locale
     * del batch e dell'associazione delle request fallisca.
     * Al ciclo successivo le request ancora NOT_WORKED vengono riprese e inviate in un nuovo batch.
     */
    @Test
    void InadBatch_ProviderAcceptedThenPersistenceFails_RestartCreatesNewBatchForRequests() {
        List<TestRequest> requests =
                createNineRequests("batch-persist-failure");

        enqueueMassiveRequests(requests);

        String firstPollingId =
                "polling-persist-first-" + UUID.randomUUID();

        String firstBatchId =
                submitBatch(firstPollingId, requests);

        dynamoDbClient.deleteItem(builder -> builder
                .tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)
                .key(Map.of(
                        "batchId",
                        AttributeValue.builder().s(firstBatchId).build(),
                        "sk",
                        AttributeValue.builder().s("-").build()
                )));

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> restored =
                    new HashMap<>(request(testRequest.correlationId()));

            restored.put(
                    "status",
                    AttributeValue.builder().s("NOT_WORKED").build()
            );

            restored.put(
                    "registryStatus",
                    AttributeValue.builder().s("INAD#NOT_WORKED").build()
            );

            restored.remove("batchId");

            putRequestItem(restored);
        }

        assertNull(
                nationalRegistriesBatchesRepository
                        .getBatch(firstBatchId)
                        .block()
        );

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("NOT_WORKED", stored.get("status").s());
            assertNull(stored.get("batchId"));
        }

        String secondPollingId =
                "polling-persist-second-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress secondResponse =
                new ResponseRequestListDigitalAddress();

        secondResponse.setId(secondPollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(secondResponse));

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(2))
                .callMassiveEService(any(RequestListDigitalAddress.class));

        String secondBatchId =
                request(requests.getFirst().correlationId())
                        .get("batchId")
                        .s();

        assertNotEquals(firstBatchId, secondBatchId);

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INAD#WORKING", stored.get("registryStatus").s());
            assertEquals(secondBatchId, stored.get("batchId").s());
        }

        NationalRegistriesBatch secondBatch =
                getBatch(secondBatchId);

        assertNotNull(secondBatch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, secondBatch.getStatus());
        assertEquals(secondPollingId, secondBatch.getPollingId());
    }

    /**
     * Simula un fallimento parziale durante l'aggiornamento delle request dopo la creazione
     * del primo batch. Le request già aggiornate rimangono associate al batch originale,
     * mentre quella rimasta NOT_WORKED viene presa da un batch successivo.
     */
    @Test
    void InadBatch_PartialRequestWorkingUpdate_RestartCreatesNewBatchOnlyForRemainingRequest() {
        List<TestRequest> requests =
                createNineRequests("partial-working");

        enqueueMassiveRequests(requests);

        String firstPollingId =
                "polling-partial-working-first-" + UUID.randomUUID();

        String firstBatchId =
                submitBatch(firstPollingId, requests);

        TestRequest requestToRestore =
                requests.get(1);

        Map<String, AttributeValue> partialRequest =
                new HashMap<>(request(requestToRestore.correlationId()));

        partialRequest.put(
                "status",
                AttributeValue.builder().s("NOT_WORKED").build()
        );

        partialRequest.put(
                "registryStatus",
                AttributeValue.builder().s("INAD#NOT_WORKED").build()
        );

        partialRequest.remove("batchId");

        putRequestItem(partialRequest);

        Map<String, AttributeValue> beforeRestart =
                request(requestToRestore.correlationId());

        assertEquals("NOT_WORKED", beforeRestart.get("status").s());
        assertNull(beforeRestart.get("batchId"));

        List<TestRequest> fillers =
                createNineRequests("partial-working-second-batch")
                        .subList(0, 8);

        enqueueMassiveRequests(fillers);

        String secondPollingId =
                "polling-partial-working-second-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress secondResponse =
                new ResponseRequestListDigitalAddress();

        secondResponse.setId(secondPollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(secondResponse));

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(2))
                .callMassiveEService(any(RequestListDigitalAddress.class));

        Map<String, AttributeValue> recovered =
                request(requestToRestore.correlationId());

        assertEquals("WORKING", recovered.get("status").s());
        assertEquals("INAD#WORKING", recovered.get("registryStatus").s());
        assertNotNull(recovered.get("batchId"));

        String secondBatchId =
                recovered.get("batchId").s();

        assertNotEquals(firstBatchId, secondBatchId);

        for (int i = 0; i < requests.size(); i++) {
            if (i == 1) {
                continue;
            }

            Map<String, AttributeValue> stored =
                    request(requests.get(i).correlationId());

            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INAD#WORKING", stored.get("registryStatus").s());
            assertEquals(firstBatchId, stored.get("batchId").s());
        }

        for (TestRequest filler : fillers) {
            Map<String, AttributeValue> stored =
                    request(filler.correlationId());

            assertEquals("WORKING", stored.get("status").s());
            assertEquals(secondBatchId, stored.get("batchId").s());
        }

        NationalRegistriesBatch firstBatch =
                getBatch(firstBatchId);

        NationalRegistriesBatch secondBatch =
                getBatch(secondBatchId);

        assertNotNull(firstBatch);
        assertNotNull(secondBatch);

        assertEquals(firstPollingId, firstBatch.getPollingId());
        assertEquals(secondPollingId, secondBatch.getPollingId());
    }

    /**
     * Verifica che due worker concorrenti tentino di acquisire lo stesso batch
     * senza produrre due lavorazioni concorrenti dello stesso elemento.
     */
    @Test
    void Polling_TwoWorkersCompeteForSameBatch_OnlyOneReservationWins() {
        List<TestRequest> requests =
                createNineRequests("polling-concurrent");
        enqueueMassiveRequests(requests);

        String pollingId =
                "polling-concurrent-" + UUID.randomUUID();

        String batchId =
                submitBatch(pollingId, requests);

        assertEquals(
                BatchesStatusEnum.REQUEST_SENT,
                getBatch(batchId).getStatus()
        );

        CompletableFuture<Void> workerOne =
                CompletableFuture.runAsync(pollingJob::polling);

        CompletableFuture<Void> workerTwo =
                CompletableFuture.runAsync(pollingJob::polling);

        CompletableFuture.allOf(
                workerOne,
                workerTwo
        ).join();

        NationalRegistriesBatch persisted =
                nationalRegistriesBatchesRepository
                        .getBatch(batchId)
                        .block();

        assertNotNull(persisted);
        assertEquals(
                BatchesStatusEnum.WORKING,
                persisted.getStatus()
        );
    }

    /**
     * Simula un'interruzione durante la persistenza dei risultati del polling:
     * alcune richieste risultano già WORKED mentre una rimane WORKING.
     * Verifica che il replay dello stesso polling completi solo la richiesta
     * ancora WORKING senza modificare i risultati già persistiti.
     */
    @Test
    void InadPolling_PartialRequestProcessing_ReplayCompletesOnlyRemainingRequest() {
        List<TestRequest> requests = createNineRequests("partial-poll");
        enqueueMassiveRequests(requests);

        String pollingId = "partial-polling-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            providerResults.add(inadResponse(requests.get(i).taxId(), "partial-" + i + "@pec.it", null));
        }

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(result));

        PnPollingEvent.Payload payload = PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(9)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();

        inadPollingService.handleMessage(payload).block();

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        Map<String, String> originalPayloads = new HashMap<>();

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId());
            assertEquals("WORKED", stored.get("status").s());
            originalPayloads.put(testRequest.correlationId(), stored.get("resultPayload").s());
        }

        TestRequest requestToReplay = requests.get(1);

        Map<String, AttributeValue> partial = new HashMap<>(request(requestToReplay.correlationId()));
        partial.put("status", AttributeValue.builder().s("WORKING").build());
        partial.put("registryStatus", AttributeValue.builder().s("INAD#WORKING").build());
        partial.remove("resultPayload");

        putRequestItem(partial);

        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("status", AttributeValue.builder().s("WORKING").build());

        putBatchItem(batch);

        assertEquals("WORKING", request(requestToReplay.correlationId()).get("status").s());

        inadPollingService.handleMessage(payload).block();

        for (int i = 0; i < requests.size(); i++) {
            TestRequest testRequest = requests.get(i);
            Map<String, AttributeValue> stored = request(testRequest.correlationId());

            assertEquals("WORKED", stored.get("status").s());
            assertNotNull(stored.get("resultPayload"));

            if (!testRequest.correlationId().equals(requestToReplay.correlationId())) {
                assertEquals(originalPayloads.get(testRequest.correlationId()), stored.get("resultPayload").s());
            }

            GatewayResponse finalPayload = readNewOutputMessage(stored.get("resultPayload").s());

            assertEquals("partial-" + i + "@pec.it", finalPayload.getPayload().getDigitalAddress().getFirst().getAddress());
        }

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    /**
     * Verifica l'esaurimento del retry INAD attraverso tre cicli reali: due reset del batch e chiusura in ERROR al terzo errore.
     */
    @Test
    void InadPolling_ErrorRetryExhaustion_AcrossRealCycles() {
        int maxRetry = 4;

        Flow flow = startWorkingInadFlow(
                "inad-error-exhaustion-" + UUID.randomUUID(),
                VALID_PF_CF
        );

        when(inadClient.checkMassiveRequestStatus(flow.pollingId()))
                .thenReturn(Mono.error(new IllegalStateException("temporary provider error")));

        for (int retry = 0; retry < maxRetry; retry++) {
            pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(
                    MessageBuilder.withPayload(pollingPayload(flow, retry, 0)).build()
            );

            Map<String, AttributeValue> batch = findBatch(flow.batchId());

            assertEquals("REQUEST_SENT", batch.get("status").s());
            assertEquals(String.valueOf(retry + 1), batch.get("errorRetryCount").n());

            makeBatchImmediatelyRetryable(flow.batchId());
            pollingJob.polling();

            assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());
        }

        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(
                MessageBuilder.withPayload(pollingPayload(flow, maxRetry, 0)).build()
        );

        assertEquals("ERROR", findBatch(flow.batchId()).get("status").s());
        assertEquals(
                "ERROR",
                findNationalRegistriesRequestsByCorrelationId(flow.correlationId())
                        .getFirst()
                        .get("status")
                        .s()
        );
    }

    /**
     * Verifica che nel flusso INAD massivo la correlazione tra request e risultati
     * rimanga corretta con risposta provider fuori ordine, CF assente,
     * stesso CF associato a più correlationId e recupero paginato delle request.
     */
    @Test
    void InadMassive_PaginationAndCorrelationRemainStableWithOutOfOrderAndMissingResults() {
        String sharedTaxId = "JSMKRV95P54H501D";

        Map<String, String> cases = new LinkedHashMap<>();

        cases.put("corr~separator-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-same-cf-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-b-" + UUID.randomUUID(), "XLPJSM93B21H501O");
        cases.put("corr-c-" + UUID.randomUUID(), "BSMGPR92R62F205X");
        cases.put("corr-missing-" + UUID.randomUUID(), "KRSJSM88S03H501A");
        cases.put("corr-filler-e-" + UUID.randomUUID(), "KPRSMP91H12F205O");
        cases.put("corr-filler-f-" + UUID.randomUUID(), "RSSMSM85E15H501L");
        cases.put("corr-filler-g-" + UUID.randomUUID(), "VRDLSM78B02F839R");
        cases.put("corr-filler-h-" + UUID.randomUUID(), "TSDSND90R10A952M");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            enqueueGatewayRequest("PF", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
        }

        String pollingId = "inad-correlation-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        String batchId = request(cases.keySet().iterator().next(), "INAD").get("batchId").s();

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();

        result.setList(List.of(
                inadResponse("BSMGPR92R62F205X", "c@pec.it", null),
                inadResponse(sharedTaxId, "shared@pec.it", null),
                inadResponse("XLPJSM93B21H501O", "b@pec.it", null),
                inadResponse("KPRSMP91H12F205O", "filler-e@pec.it", null),
                inadResponse("RSSMSM85E15H501L", "filler-f@pec.it", null),
                inadResponse("VRDLSM78B02F839R", "filler-g@pec.it", null),
                inadResponse("TSDSND90R10A952M", "filler-h@pec.it", null)
        ));

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(result));

        inadPollingService.handleMessage(
                payload(
                        GatewayDownstreamService.INAD,
                        batchId,
                        pollingId,
                        9,
                        0,
                        0
                )
        ).block();

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Map<String, AttributeValue> stored = request(entry.getKey(), "INAD");

            assertEquals("WORKED", stored.get("status").s());
            assertNotNull(stored.get("resultPayload"));

            GatewayResponse resultPayload = readNewOutputMessage(stored.get("resultPayload").s());

            assertEquals(entry.getKey(), resultPayload.getPayload().getCorrelationId());

            if (sharedTaxId.equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("shared@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("XLPJSM93B21H501O".equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("b@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("BSMGPR92R62F205X".equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("c@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("KRSJSM88S03H501A".equals(entry.getValue())) {
                assertNotNull(resultPayload.getPayload().getDigitalAddress());
                assertTrue(resultPayload.getPayload().getDigitalAddress().isEmpty());
            }
        }

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    private GatewayResponse runMassiveFunctionalScenario(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));
        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest(
                    VALID_FILLER_CFS.get(i),
                    "functional-filler-" + i + "-" + UUID.randomUUID()
            ));
        }
        enqueueMassiveRequests(requests);
        String pollingId = "polling-functional-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();
        if (targetResult != null) {
            providerResults.add(targetResult);
        }
        for (int i = 1; i < requests.size(); i++) {
            TestRequest filler = requests.get(i);
            providerResults.add(inadResponse(filler.taxId(), "functional-filler-" + i + "@pec.it", null));
        }
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        poll(batchId, pollingId, requests.size());
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        Map<String, AttributeValue> stored = request(correlationId);
        assertEquals("WORKED", stored.get("status").s());
        assertEquals("INAD#WORKED", stored.get("registryStatus").s());
        assertEquals(batchId, stored.get("batchId").s());
        assertNotNull(stored.get("resultPayload"));
        verify(inadClient, never()).callEService(any(), any());
        verifyNoInteractions(ipaClient, anprClient);
        return readNewOutputMessage(stored.get("resultPayload").s());
    }

    private Map<String, AttributeValue> request(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> registry.equals(item.get("registry").s()))
                .findFirst().orElseThrow();
    }

    private PnPollingEvent.Payload payload(GatewayDownstreamService registry, String batchId, String pollingId, int batchSize, int errorRetry, int inProgressRetry) {
        return PnPollingEvent.Payload.builder().registry(registry).batchId(batchId).pollingId(pollingId).batchSize(batchSize)
                .errorRetryCount(errorRetry).inProgressRetryCount(inProgressRetry).build();
    }

    private Flow startWorkingInadFlow(String correlationId, String taxId) {
        enqueueGatewayRequest("PF", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        for (int i = 0; i < 8; i++) {
            String fillerCorrelationId = "inad-flow-filler-" + i + "-" + UUID.randomUUID();
            String fillerTaxId = VALID_FILLER_CFS.get(i);
            enqueueGatewayRequest("PF", fillerTaxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, fillerCorrelationId);
            processNextGatewayInputMessage();
        }

        String pollingId = "inad-polling-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        Map<String, AttributeValue> request = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
        String batchId = request.get("batchId").s();

        pollingJob.polling();

        assertEquals("WORKING", findBatch(batchId).get("status").s());

        return new Flow(correlationId, batchId, pollingId);
    }

    private PnPollingEvent.Payload pollingPayload(Flow flow, int errorRetryCount, int inProgressRetryCount) {
        return PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(flow.batchId())
                .pollingId(flow.pollingId())
                .batchSize(9)
                .errorRetryCount(errorRetryCount)
                .inProgressRetryCount(inProgressRetryCount)
                .build();
    }

    private void makeBatchImmediatelyRetryable(String batchId) {
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("attemptAfter", AttributeValue.builder().s(Instant.now().minusSeconds(1).toString()).build());
        putBatchItem(batch);
    }


    private record Flow(String correlationId, String batchId, String pollingId) {
    }

    private List<TestRequest> createNineRequests(String prefix) {
        List<TestRequest> requests =
                new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            requests.add(new TestRequest(
                    VALID_FILLER_CFS.get(i),
                    prefix + i + "-" + UUID.randomUUID()
            ));
        }

        return requests;
    }

    private void enqueueMassiveRequests(
            List<TestRequest> requests
    ) {
        for (TestRequest testRequest : requests) {
            AddressOKDto response =
                    enqueueGatewayRequest(
                            "PF",
                            testRequest.taxId(),
                            AddressRequestBodyFilterDto
                                    .DomicileTypeEnum
                                    .DIGITAL,
                            testRequest.correlationId()
                    );

            processNextGatewayInputMessage();

            assertEquals(
                    testRequest.correlationId(),
                    response.getCorrelationId()
            );

            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals(
                    "NOT_WORKED",
                    stored.get("status").s()
            );
            assertEquals(
                    "INAD#NOT_WORKED",
                    stored.get("registryStatus").s()
            );
            assertNull(stored.get("batchId"));
            assertNull(stored.get("resultPayload"));
        }
    }

    private String submitBatch(
            String pollingId,
            List<TestRequest> requests
    ) {
        ResponseRequestListDigitalAddress response =
                new ResponseRequestListDigitalAddress();
        response.setId(pollingId);

        when(inadClient.callMassiveEService(
                any(RequestListDigitalAddress.class)
        )).thenReturn(Mono.just(response));

        inadBatchRequestJob.batchRequest();

        String batchId =
                assertSameWorkingBatch(requests);

        NationalRegistriesBatch batch =
                getBatch(batchId);

        assertNotNull(batch);
        assertEquals(
                BatchesStatusEnum.REQUEST_SENT,
                batch.getStatus()
        );
        assertEquals(
                pollingId,
                batch.getPollingId()
        );

        return batchId;
    }

    private String assertSameWorkingBatch(
            List<TestRequest> requests
    ) {
        String batchId = null;

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals(
                    "WORKING",
                    stored.get("status").s()
            );
            assertEquals(
                    "INAD#WORKING",
                    stored.get("registryStatus").s()
            );
            assertNotNull(stored.get("batchId"));

            if (batchId == null) {
                batchId =
                        stored.get("batchId").s();
            } else {
                assertEquals(
                        batchId,
                        stored.get("batchId").s()
                );
            }
        }

        assertNotNull(batchId);

        return batchId;
    }

    private void poll(
            String batchId,
            String pollingId,
            int batchSize
    ) {
        inadPollingService.handleMessage(
                PnPollingEvent.Payload.builder()
                        .registry(GatewayDownstreamService.INAD)
                        .batchId(batchId)
                        .pollingId(pollingId)
                        .batchSize(batchSize)
                        .errorRetryCount(0)
                        .inProgressRetryCount(0)
                        .build()
        ).block();
    }

    private ResponseStatusListDigitalAddress status(
            StatusProcessingRequest state
    ) {
        ResponseStatusListDigitalAddress response =
                new ResponseStatusListDigitalAddress();

        response.setState(state);

        return response;
    }

    private Map<String, AttributeValue> request(
            String correlationId
    ) {
        return findNationalRegistriesRequestsByCorrelationId(
                correlationId
        ).getFirst();
    }

    private NationalRegistriesBatch getBatch(
            String batchId
    ) {
        return nationalRegistriesBatchesRepository
                .getBatch(batchId)
                .block();
    }

    private Map<String, AttributeValue> findBatch(
            String batchId
    ) {
        return dynamoDbClient.getItem(builder -> builder
                        .tableName(
                                NATIONAL_REGISTRIES_BATCHES_TABLE
                        )
                        .key(Map.of(
                                "batchId",
                                AttributeValue.builder()
                                        .s(batchId)
                                        .build(),
                                "sk",
                                AttributeValue.builder()
                                        .s("-")
                                        .build()
                        )))
                .item();
    }

    private void putRequestItem(
            Map<String, AttributeValue> item
    ) {
        dynamoDbClient.putItem(builder -> builder
                .tableName(
                        NATIONAL_REGISTRIES_REQUESTS_TABLE
                )
                .item(item));
    }

    private void putBatchItem(
            Map<String, AttributeValue> item
    ) {
        dynamoDbClient.putItem(builder -> builder
                .tableName(
                        NATIONAL_REGISTRIES_BATCHES_TABLE
                )
                .item(item));
    }

    private record TestRequest(
            String taxId,
            String correlationId
    ) {
    }
}