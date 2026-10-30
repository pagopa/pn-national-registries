package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL sul flusso massivo INAD.
 *
 * <p>Copre gli esiti funzionali del batch massivo, gestione degli errori,
 * risultati duplicati e scenari di resilienza relativi a restart,
 * persistenza parziale, concorrenza tra worker e replay del polling.</p>
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *     <li>{@code pn.national-registries.enable-pf-pec-fallback-flow=false}</li>
 *     <li>{@code pn.national-registries.enable-inad-massive-api=true}</li>
 *     <li>{@code pn.national-registries.inad.max-batch-request-size=9}</li>
 *     <li>{@code pn.national-registries.query-limit=2}</li>
 *     <li>{@code pn.national-registries.inad.oldest-request-max-waiting-seconds=60}</li>
 *     <li>{@code pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0}</li>
 *     <li>{@code pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0}</li>
 *     <li>{@code pn.national-registries.inad.batch-polling-max-retry=5}</li>
 *     <li>{@code pn.national-registries.inad.batch-polling-in-progress-max-retry=10}</li>
 *     <li>{@code pn.national-registries.inad.polling-error-retry-after=1}</li>
 *     <li>{@code pn.national-registries.inad.polling-in-progress-retry-after=1}</li>
 * </ul>
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
public class DigitalAddressPFMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_FOUND = "RSSMRA80A01H501A";
    private static final String PF_NOT_FOUND = "RSSMRA80A01H501B";
    private static final String PF_PROFESSIONAL_ONLY = "RSSMRA80A01H501C";
    private static final String PF_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A01H501D";
    private static final String PF_INVALID_PERSONAL = "RSSMRA80A01H501E";
    private static final String PF_MULTIPLE_PROFESSIONALS = "RSSMRA80A01H501F";
    private static final String PF_EMPTY = "RSSMRA80A01H501G";
    private static final String PF_EXPIRED_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A01H501H";
    private static final String PF_EXPIRED_PERSONAL = "RSSMRA80A01H501I";

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
     * Verifica end-to-end il batch massivo PF INAD con i principali esiti funzionali:
     * PEC personale valida, not found, PEC professionali, indirizzi invalidi,
     * indirizzi scaduti e risultato vuoto.
     */
    @Test
    void PF_Digital_INAD_Massive_AllFunctionalScenarios() {
        String foundCorrelationId = "pf-found-" + UUID.randomUUID();
        String notFoundCorrelationId = "pf-not-found-" + UUID.randomUUID();
        String professionalOnlyCorrelationId = "pf-professional-only-" + UUID.randomUUID();
        String personalAndProfessionalCorrelationId = "pf-personal-professional-" + UUID.randomUUID();
        String invalidPersonalCorrelationId = "pf-invalid-personal-" + UUID.randomUUID();
        String multipleProfessionalsCorrelationId = "pf-multiple-prof-" + UUID.randomUUID();
        String emptyCorrelationId = "pf-empty-" + UUID.randomUUID();
        String expiredPersonalAndProfessionalCorrelationId =
                "pf-expired-personal-professional-" + UUID.randomUUID();
        String expiredPersonalCorrelationId =
                "pf-expired-personal-" + UUID.randomUUID();

        List<TestRequest> testRequests = List.of(
                new TestRequest(PF_FOUND, foundCorrelationId),
                new TestRequest(PF_NOT_FOUND, notFoundCorrelationId),
                new TestRequest(PF_PROFESSIONAL_ONLY, professionalOnlyCorrelationId),
                new TestRequest(PF_PERSONAL_AND_PROFESSIONAL, personalAndProfessionalCorrelationId),
                new TestRequest(PF_INVALID_PERSONAL, invalidPersonalCorrelationId),
                new TestRequest(PF_MULTIPLE_PROFESSIONALS, multipleProfessionalsCorrelationId),
                new TestRequest(PF_EMPTY, emptyCorrelationId),
                new TestRequest(
                        PF_EXPIRED_PERSONAL_AND_PROFESSIONAL,
                        expiredPersonalAndProfessionalCorrelationId
                ),
                new TestRequest(PF_EXPIRED_PERSONAL, expiredPersonalCorrelationId)
        );

        enqueueMassiveRequests(testRequests);

        String pollingId = "polling-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse =
                new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> captor =
                ArgumentCaptor.forClass(RequestListDigitalAddress.class);

        verify(inadClient).callMassiveEService(captor.capture());

        assertEquals(
                testRequests.stream().map(TestRequest::taxId).toList(),
                captor.getValue().getCodiciFiscali()
        );

        String batchId = assertSameWorkingBatch(testRequests);

        NationalRegistriesBatch batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());

        pollingJob.polling();

        assertEquals(BatchesStatusEnum.WORKING, getBatch(batchId).getStatus());

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        ElementDigitalAddress expiredPersonalWithProfessional =
                inadAddress("expired-personal@pec.it", null);
        expiredPersonalWithProfessional.getUsageInfo()
                .setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ElementDigitalAddress expiredPersonal =
                inadAddress("expired-only@pec.it", null);
        expiredPersonal.getUsageInfo()
                .setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(
                inadResponse(PF_FOUND, "pf@pec.it", null),
                inadResponse(
                        PF_PROFESSIONAL_ONLY,
                        List.of(inadAddress("professional@pec.it", "INGEGNERE"))
                ),
                inadResponse(
                        PF_PERSONAL_AND_PROFESSIONAL,
                        List.of(
                                inadAddress("personal@pec.it", null),
                                inadAddress("professional@pec.it", "INGEGNERE")
                        )
                ),
                inadResponse(
                        PF_INVALID_PERSONAL,
                        List.of(
                                inadAddress("2026-01-01", null),
                                inadAddress("professional@pec.it", "INGEGNERE")
                        )
                ),
                inadResponse(
                        PF_MULTIPLE_PROFESSIONALS,
                        List.of(
                                inadAddress("prof1@pec.it", "INGEGNERE"),
                                inadAddress("prof2@pec.it", "ARCHITETTO")
                        )
                ),
                inadResponse(PF_EMPTY, List.of()),
                inadResponse(
                        PF_EXPIRED_PERSONAL_AND_PROFESSIONAL,
                        List.of(
                                expiredPersonalWithProfessional,
                                inadAddress(
                                        "professional-expired-case@pec.it",
                                        "INGEGNERE"
                                )
                        )
                ),
                inadResponse(
                        PF_EXPIRED_PERSONAL,
                        List.of(expiredPersonal)
                )
        ));

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(result));

        poll(batchId, pollingId, 9);

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        Map<String, AttributeValue> found =
                request(foundCorrelationId);
        assertTrue(found.get("resultPayload").s().contains("pf@pec.it"));

        Map<String, AttributeValue> notFound =
                request(notFoundCorrelationId);
        assertFalse(notFound.get("resultPayload").s().contains("@pec.it"));

        Map<String, AttributeValue> professional =
                request(professionalOnlyCorrelationId);
        assertFalse(
                professional.get("resultPayload").s()
                        .contains("professional@pec.it")
        );

        Map<String, AttributeValue> personalAndProfessional =
                request(personalAndProfessionalCorrelationId);
        assertTrue(
                personalAndProfessional.get("resultPayload").s()
                        .contains("personal@pec.it")
        );
        assertFalse(
                personalAndProfessional.get("resultPayload").s()
                        .contains("professional@pec.it")
        );

        Map<String, AttributeValue> invalidPersonal =
                request(invalidPersonalCorrelationId);
        assertFalse(
                invalidPersonal.get("resultPayload").s()
                        .contains("2026-01-01")
        );

        Map<String, AttributeValue> multipleProfessionals =
                request(multipleProfessionalsCorrelationId);
        assertFalse(
                multipleProfessionals.get("resultPayload").s()
                        .contains("prof1@pec.it")
        );
        assertFalse(
                multipleProfessionals.get("resultPayload").s()
                        .contains("prof2@pec.it")
        );

        Map<String, AttributeValue> expiredBoth =
                request(expiredPersonalAndProfessionalCorrelationId);
        assertFalse(
                expiredBoth.get("resultPayload").s()
                        .contains("expired-personal@pec.it")
        );

        Map<String, AttributeValue> expired =
                request(expiredPersonalCorrelationId);
        assertFalse(
                expired.get("resultPayload").s()
                        .contains("expired-only@pec.it")
        );

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("WORKED", stored.get("status").s());
            assertEquals("INAD#WORKED", stored.get("registryStatus").s());
            assertEquals(batchId, stored.get("batchId").s());
            assertNotNull(stored.get("resultPayload"));
        }

        verify(inadClient, never()).callEService(any(), any());
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica che, quando INAD restituisce più risultati per lo stesso codice fiscale,
     * venga mantenuto il primo risultato restituito dal provider.
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

        CodeSqsDto payload =
                readOutputMessage(stored.get("resultPayload").s());

        assertEquals(
                "first@pec.it",
                payload.getDigitalAddress().getFirst().getAddress()
        );
        assertFalse(
                stored.get("resultPayload").s().contains("second@pec.it")
        );
    }

    /**
     * Verifica che un errore temporaneo durante il recupero del risultato INAD
     * incrementi il retry del batch lasciando le richieste in stato WORKING.
     */
    @Test
    void PF_Digital_INAD_Massive_Error() {
        List<TestRequest> requests = createNineRequests("pf-error");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-error-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.error(
                        new RuntimeException("INAD unavailable")
                ));

        poll(batchId, pollingId, 9);

        NationalRegistriesBatch batch = getBatch(batchId);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(1, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("WORKING", stored.get("status").s());
            assertNull(stored.get("resultPayload"));
        }
    }

    /**
     * Simula un crash immediatamente dopo che il provider ha accettato il batch
     * rimuovendo il record locale del batch già persistito. Verifica che il riavvio
     * del batch job non provochi una seconda sottomissione delle stesse richieste al provider.
     */
    @Test
    void InadBatch_ProviderAcceptedThenBatchPersistenceFails_RestartDoesNotDuplicateProviderSubmission() {
        List<TestRequest> requests =
                createNineRequests("batch-persist-failure");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-persist-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);

        dynamoDbClient.deleteItem(builder -> builder
                .tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)
                .key(Map.of(
                        "batchId",
                        AttributeValue.builder().s(batchId).build(),
                        "sk",
                        AttributeValue.builder().s("-").build()
                )));

        assertNull(
                nationalRegistriesBatchesRepository
                        .getBatch(batchId)
                        .block()
        );

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(1))
                .callMassiveEService(any(RequestListDigitalAddress.class));

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("WORKING", stored.get("status").s());
            assertEquals(batchId, stored.get("batchId").s());
        }
    }

    /**
     * Simula un crash durante l'aggiornamento delle richieste dopo la creazione
     * del batch: una richiesta viene riportata allo stato precedente mentre le altre
     * risultano già WORKING. Verifica che il riavvio non produca un secondo batch
     * per le richieste già associate al batch originale.
     */
    @Test
    void InadBatch_PartialRequestWorkingUpdate_RestartKeepsSingleBatch() {
        List<TestRequest> requests =
                createNineRequests("partial-working");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-partial-working-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);

        TestRequest requestToRestore = requests.get(1);

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

        assertEquals(
                "NOT_WORKED",
                beforeRestart.get("status").s()
        );
        assertNull(beforeRestart.get("batchId"));

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(1))
                .callMassiveEService(any(RequestListDigitalAddress.class));

        for (int i = 0; i < requests.size(); i++) {
            Map<String, AttributeValue> stored =
                    request(requests.get(i).correlationId());

            if (i == 1) {
                assertEquals(
                        "NOT_WORKED",
                        stored.get("status").s()
                );
            } else {
                assertEquals(
                        "WORKING",
                        stored.get("status").s()
                );
                assertEquals(
                        batchId,
                        stored.get("batchId").s()
                );
            }
        }
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
     * Simula un crash dopo la persistenza parziale del risultato del polling:
     * una richiesta viene riportata a WORKING mentre le altre restano WORKED.
     * Verifica che il replay dello stesso polling completi la richiesta residua
     * senza modificare i payload già prodotti.
     */
    @Test
    void InadPolling_PartialPersistenceFailure_ReplayCompletesOnlyRemainingRequest() {
        List<TestRequest> requests =
                createNineRequests("partial-poll");

        enqueueMassiveRequests(requests);

        String pollingId =
                "partial-polling-" + UUID.randomUUID();

        String batchId =
                submitBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(
                        status(StatusProcessingRequest.DISPONIBILE)
                ));

        List<ResponseRequestDigitalAddress> providerResults =
                new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            providerResults.add(
                    inadResponse(
                            requests.get(i).taxId(),
                            "partial-" + i + "@pec.it",
                            null
                    )
            );
        }

        ResponseListDigitalAddress result =
                new ResponseListDigitalAddress();
        result.setList(providerResults);

        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(result));

        PnPollingEvent.Payload payload =
                PnPollingEvent.Payload.builder()
                        .registry(GatewayDownstreamService.INAD)
                        .batchId(batchId)
                        .pollingId(pollingId)
                        .batchSize(9)
                        .errorRetryCount(0)
                        .inProgressRetryCount(0)
                        .build();

        inadPollingService
                .handleMessage(payload)
                .block();

        assertEquals(
                BatchesStatusEnum.WORKED,
                getBatch(batchId).getStatus()
        );

        Map<String, String> originalPayloads =
                new HashMap<>();

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals("WORKED", stored.get("status").s());

            originalPayloads.put(
                    testRequest.correlationId(),
                    stored.get("resultPayload").s()
            );
        }

        TestRequest requestToReplay =
                requests.get(1);

        Map<String, AttributeValue> partial =
                new HashMap<>(
                        request(requestToReplay.correlationId())
                );

        partial.put(
                "status",
                AttributeValue.builder()
                        .s("WORKING")
                        .build()
        );
        partial.put(
                "registryStatus",
                AttributeValue.builder()
                        .s("INAD#WORKING")
                        .build()
        );
        partial.remove("resultPayload");

        putRequestItem(partial);

        Map<String, AttributeValue> batch =
                new HashMap<>(findBatch(batchId));

        batch.put(
                "status",
                AttributeValue.builder()
                        .s("WORKING")
                        .build()
        );

        putBatchItem(batch);

        assertEquals(
                "WORKING",
                request(requestToReplay.correlationId())
                        .get("status")
                        .s()
        );

        inadPollingService
                .handleMessage(payload)
                .block();

        for (int i = 0; i < requests.size(); i++) {
            TestRequest testRequest =
                    requests.get(i);

            Map<String, AttributeValue> stored =
                    request(testRequest.correlationId());

            assertEquals(
                    "WORKED",
                    stored.get("status").s()
            );

            assertNotNull(
                    stored.get("resultPayload")
            );

            if (!testRequest.correlationId()
                    .equals(requestToReplay.correlationId())) {

                assertEquals(
                        originalPayloads.get(
                                testRequest.correlationId()
                        ),
                        stored.get("resultPayload").s()
                );
            }

            CodeSqsDto finalPayload =
                    readOutputMessage(
                            stored.get("resultPayload").s()
                    );

            assertEquals(
                    "partial-" + i + "@pec.it",
                    finalPayload
                            .getDigitalAddress()
                            .getFirst()
                            .getAddress()
            );
        }

        assertEquals(
                BatchesStatusEnum.WORKED,
                getBatch(batchId).getStatus()
        );
    }

    /**
     * Verifica che correlationId con apici, virgolette e caratteri non ASCII attraversi API, SQS, persistenza e resultPayload senza alterazioni di serializzazione.
     */
    @Test
    void InadResultPayload_SpecialCharactersArePreservedAcrossAsyncFlow() {
        String correlationId = "corr-'\"-è-" + UUID.randomUUID();
        Flow flow = startWorkingInadFlow(correlationId, VALID_PF_CF);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(inadResponse(VALID_PF_CF, "special@pec.it", null)));
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));

        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(pollingPayload(flow, 0, 0)).build());

        Map<String, AttributeValue> request = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
        CodeSqsDto resultPayload = readOutputMessage(request.get("resultPayload").s());
        assertEquals(correlationId, resultPayload.getCorrelationId());
        assertEquals("special@pec.it", resultPayload.getDigitalAddress().getFirst().getAddress());
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
     * Verifica correlazione e paginazione INAD con risposta fuori ordine, CF duplicato, CF mancante, stesso CF su due correlationId e correlationId contenente il separatore '~'.
     */
    /**
     * Verifica correlazione e paginazione INAD con risposta fuori ordine, CF duplicato,
     * CF mancante, stesso CF su due correlationId e correlationId contenente il separatore '~'.
     */
    @Test
    void InadMassive_PaginationAndCorrelationRemainStableWithDuplicatesAndMissingCf() {
        String duplicateTaxId = "RSSMRA80A01H501A";

        Map<String, String> cases = new LinkedHashMap<>();

        cases.put("corr~separator-" + UUID.randomUUID(), duplicateTaxId);
        cases.put("corr-duplicate-" + UUID.randomUUID(), duplicateTaxId);
        cases.put("corr-b-" + UUID.randomUUID(), "RSSMRA80A01H501B");
        cases.put("corr-c-" + UUID.randomUUID(), "RSSMRA80A01H501C");
        cases.put("corr-missing-" + UUID.randomUUID(), "RSSMRA80A01H501D");
        cases.put("corr-filler-e-" + UUID.randomUUID(), "RSSMRA80A01H501E");
        cases.put("corr-filler-f-" + UUID.randomUUID(), "RSSMRA80A01H501F");
        cases.put("corr-filler-g-" + UUID.randomUUID(), "RSSMRA80A01H501G");
        cases.put("corr-filler-h-" + UUID.randomUUID(), "RSSMRA80A01H501H");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            enqueueGatewayRequest("PF", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
        }

        String pollingId = "inad-correlation-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();

        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        String batchId = request(cases.keySet().iterator().next(), "INAD").get("batchId").s();

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();

        result.setList(List.of(
                inadResponse("RSSMRA80A01H501C", "c@pec.it", null),
                inadResponse(duplicateTaxId, "first-duplicate@pec.it", null),
                inadResponse(duplicateTaxId, "second-duplicate@pec.it", null),
                inadResponse("RSSMRA80A01H501B", "b@pec.it", null),
                inadResponse("RSSMRA80A01H501E", "filler-e@pec.it", null),
                inadResponse("RSSMRA80A01H501F", "filler-f@pec.it", null),
                inadResponse("RSSMRA80A01H501G", "filler-g@pec.it", null),
                inadResponse("RSSMRA80A01H501H", "filler-h@pec.it", null)
        ));

        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));

        inadPollingService.handleMessage(payload(GatewayDownstreamService.INAD, batchId, pollingId, 9, 0, 0)).block();

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Map<String, AttributeValue> request = request(entry.getKey(), "INAD");

            assertEquals("WORKED", request.get("status").s());

            CodeSqsDto resultPayload = readOutputMessage(request.get("resultPayload").s());

            assertEquals(entry.getKey(), resultPayload.getCorrelationId());

            if (duplicateTaxId.equals(entry.getValue())) {
                assertEquals("first-duplicate@pec.it", resultPayload.getDigitalAddress().getFirst().getAddress());
            }

            if ("RSSMRA80A01H501D".equals(entry.getValue())) {
                assertTrue(resultPayload.getDigitalAddress().isEmpty());
            }
        }
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
            String fillerTaxId = "RSSMRA80A01H50" + i + "U";
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
            requests.add(
                    new TestRequest(
                            "RSSMRA80A01H50" + i + "U",
                            prefix + "-" + i + "-" + UUID.randomUUID()
                    )
            );
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
