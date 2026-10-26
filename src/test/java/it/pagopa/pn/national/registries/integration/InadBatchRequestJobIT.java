package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.max-batch-request-size=3",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=60",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0"
})
class InadBatchRequestJobIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    // Verifica che tutte le richieste eleggibili siano aggregate anche attraverso i confini delle pagine DynamoDB.
    // Assertions: vengono effettuate due chiamate con tutti i codici fiscali; i due batch risultano inviati,
    // di registry INAD e con polling ID, riferimenti e date di tentativo valorizzati; ogni richiesta passa a WORKING
    // ed è associata a un batch.
    @Test
    void sendsAllEligiblePagesAndAssociatesEachRequestWithItsBatch() throws Exception {
        List<NationalRegistriesRequest> requests = persistRequests(6, Instant.now().minusSeconds(10));
        AtomicInteger pollingId = new AtomicInteger();
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenAnswer(invocation -> Mono.just(inadBatchResponse("polling-" + pollingId.incrementAndGet())));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient, times(2)).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of(
                        requests.get(0).getTaxId(),
                        requests.get(1).getTaxId(),
                        requests.get(2).getTaxId()),
                requestCaptor.getAllValues().getFirst().getCodiciFiscali());
        assertEquals(List.of(
                        requests.get(3).getTaxId(),
                        requests.get(4).getTaxId(),
                        requests.get(5).getTaxId()),
                requestCaptor.getAllValues().get(1).getCodiciFiscali());
        assertEquals(2, findBatches().size());
        assertEquals(2, findBatches().stream()
                .filter(batch -> "REQUEST_SENT".equals(batch.get("status").s()))
                .filter(batch -> "3".equals(batch.get("batchSize").n()))
                .count());
        findBatches().forEach(batch -> {
            assertEquals("INAD", batch.get("registry").s());
            assertNotNull(batch.get("pollingId").s());
            assertEquals(batch.get("batchId").s(), batch.get("practicalReference").s());
            assertNotNull(batch.get("attemptAfter").s());
        });

        for (NationalRegistriesRequest request : requests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertNotNull(storedRequest.get("batchId").s());
        }
    }

    // Verifica che l'esecuzione senza richieste INAD pendenti non avvii chiamate al provider né crei batch.
    // Assertions: il client INAD non riceve interazioni e non risultano batch.
    @Test
    void doesNothingWhenThereAreNoPendingInadRequests() throws Exception {
        inadBatchRequestJob.batchRequest();

        verifyNoInteractions(inadClient);
        assertEquals(0, findBatches().size());
    }

    // Verifica che un gruppo incompleto e recente resti disponibile per un'esecuzione successiva.
    // Assertions: il client INAD non viene invocato, la richiesta resta non lavorata e senza batch e non vengono creati batch.
    @Test
    void leavesIncompleteRecentGroupAvailableForNextExecution() throws Exception {
        NationalRegistriesRequest request = persistRequests(1, Instant.now()).getFirst();

        inadBatchRequestJob.batchRequest();

        verifyNoInteractions(inadClient);
        assertTrueNotWorkedWithoutBatchId(request);
        assertEquals(0, findBatches().size());
    }

    // Verifica che, dopo aver inviato un batch completo, il job lasci disponibile il residuo recente.
    // Assertions: viene inviata solo la prima pagina, viene creato un batch e la richiesta residua resta non lavorata e senza batch.
    @Test
    void stopsAfterAnEligiblePageWhenTheNextIncompletePageIsStillRecent() throws Exception {
        List<NationalRegistriesRequest> requests = persistRequests(4, Instant.now());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(inadBatchResponse("polling-id")));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> requestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(requestCaptor.capture());
        assertEquals(List.of(requests.getFirst().getTaxId(), requests.get(1).getTaxId(), requests.get(2).getTaxId()),
                requestCaptor.getValue().getCodiciFiscali());
        assertEquals(1, findBatches().size());
        assertTrueNotWorkedWithoutBatchId(requests.get(3));
    }

    // Verifica che un gruppo incompleto venga inviato quando la richiesta più vecchia supera il tempo massimo di attesa.
    // Assertions: il client INAD viene invocato, viene creato un batch e la richiesta passa a WORKING con un batch associato.
    @Test
    void sendsIncompleteGroupWhenItsOldestRequestExceededTheMaximumWaitingTime() throws Exception {
        NationalRegistriesRequest request = persistRequests(1, Instant.now().minusSeconds(61)).getFirst();
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(inadBatchResponse("polling-id")));

        inadBatchRequestJob.batchRequest();

        verify(inadClient).callMassiveEService(any(RequestListDigitalAddress.class));
        assertEquals(1, findBatches().size());
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("WORKING", storedRequest.get("status").s());
        assertNotNull(storedRequest.get("batchId").s());
    }

    // Verifica la gestione di un errore restituito da INAD durante l'invio del batch.
    // Assertions: l'eccezione originale viene propagata, non vengono creati batch e le richieste restano non lavorate e senza batch.
    @Test
    void propagatesInadErrorWithoutPersistingBatchesOrChangingRequests() {
        List<NationalRegistriesRequest> requests = persistRequests(3, Instant.now());
        IllegalStateException inadError = new IllegalStateException("INAD unavailable");
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.error(inadError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> inadBatchRequestJob.batchRequest());

        assertEquals(inadError, thrown);
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertTrueNotWorkedWithoutBatchId);
    }

    @Test
    void propagatesNonBadRequestPnExceptionWithoutPersistingBatchesOrChangingRequests() {
        List<NationalRegistriesRequest> requests = persistRequests(3, Instant.now());
        PnNationalRegistriesException inadError = pnException("INAD unavailable", HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.error(inadError));

        PnNationalRegistriesException thrown = assertThrows(PnNationalRegistriesException.class, () -> inadBatchRequestJob.batchRequest());

        assertEquals(inadError, thrown);
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertTrueNotWorkedWithoutBatchId);
    }

    @Test
    void marksAllRequestsAsErrorAndStopsFlowWhenInadReturnsBadRequest() {
        List<NationalRegistriesRequest> requests = persistRequests(3, Instant.now());
        PnNationalRegistriesException badRequest = pnException("Invalid massive request", HttpStatus.BAD_REQUEST.value());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.error(badRequest));

        inadBatchRequestJob.batchRequest();

        verify(inadClient).callMassiveEService(any(RequestListDigitalAddress.class));
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertErrorWithoutBatchId);
    }

    // Verifica il comportamento se, dopo l'accettazione del batch da parte di INAD, l'aggiornamento condizionale della richiesta fallisce.
    // Assertions: viene propagata un'eccezione, risulta un batch e la richiesta conserva lo stato WORKING e il batch ID assegnato dalla concorrenza.
    @Test
    void propagatesTheConditionalUpdateFailureAfterTheProviderAcceptedTheBatch() {
        NationalRegistriesRequest request = persistRequests(1, Instant.now().minusSeconds(61)).getFirst();
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenAnswer(invocation -> nationalRegistriesRequestsRepository.updateStatusToWorking(
                                request.getCorrelationId(),
                                request.getCreatedAt().toString(),
                                "concurrent-batch",
                                GatewayDownstreamService.INAD)
                        .then(Mono.just(inadBatchResponse("polling-id"))));

        assertThrows(RuntimeException.class, () -> inadBatchRequestJob.batchRequest());

        assertEquals(1, findBatches().size());
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("WORKING", storedRequest.get("status").s());
        assertEquals("concurrent-batch", storedRequest.get("batchId").s());
    }

    // Verifica che una risposta INAD priva di polling ID venga rifiutata senza persistere modifiche.
    // Assertions: viene sollevata una PnInternalException con il dettaglio atteso, non vengono creati batch
    // e le richieste restano non lavorate e senza batch.
    @Test
    void rejectsInadResponseWithoutPollingIdentifierWithoutPersistingChanges() {
        List<NationalRegistriesRequest> requests = persistRequests(3, Instant.now());
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class)))
                .thenReturn(Mono.just(new ResponseRequestListDigitalAddress()));

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> inadBatchRequestJob.batchRequest());

        assertNotNull(thrown.getProblem().getDetail());
        assertTrue(thrown.getProblem().getDetail()
                .startsWith("INAD returned no polling identifier for batch "));
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertTrueNotWorkedWithoutBatchId);
    }


    //crea e salva count richieste nel repository e restituisce la lista delle richieste create.
    private List<NationalRegistriesRequest> persistRequests(int count, Instant firstCreatedAt) {
        return IntStream.range(0, count)
                .mapToObj(index -> request(index, firstCreatedAt.plus(index, ChronoUnit.MILLIS)))
                .peek(request -> nationalRegistriesRequestsRepository.putRequest(request).block())
                .toList();
    }

    private NationalRegistriesRequest request(int index, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId("inad-batch-job-" + index + "-" + java.util.UUID.randomUUID());
        request.setTaxId("RSSMRA80A01H50" + index + "U");
        request.setCreatedAt(createdAt);
        request.setRegistry(GatewayDownstreamService.INAD);
        request.setStatus(RequestStatusEnum.NOT_WORKED);
        request.setRegistryStatus("INAD#NOT_WORKED");
        request.setAttemptAfter(formatInstant(createdAt.minus(10, ChronoUnit.MINUTES)));
        return request;
    }

    private List<Map<String, AttributeValue>> findBatches() {
        return dynamoDbClient.scan(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)).items();
    }

    //verifica che la richiesta, dopo il test, sia ancora non lavorata e non assegnata a un batch.
    //Cerca il record su DynamoDB tramite correlationId, poi controlla che:
    //status sia NOT_WORKED;
    //registryStatus sia INAD#NOT_WORKED;
    //batchId sia assente (null).
    private void assertTrueNotWorkedWithoutBatchId(NationalRegistriesRequest request) {
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INAD#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private void assertErrorWithoutBatchId(NationalRegistriesRequest request) {
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("ERROR", storedRequest.get("status").s());
        assertEquals("INAD#ERROR", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    //crea una risposta fittizia del batch INAD con l'ID di polling specificato.
    private ResponseRequestListDigitalAddress inadBatchResponse(String pollingId) {
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);
        return response;
    }

    private PnNationalRegistriesException pnException(String message, int statusCode) {
        return new PnNationalRegistriesException(message, statusCode, "error", null, null, Charset.defaultCharset(), null);
    }

}