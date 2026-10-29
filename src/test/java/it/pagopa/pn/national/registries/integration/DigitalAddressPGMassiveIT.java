package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali: Copre il full flow PG con fallback IniPEC e INAD massive, includendo selezione degli indirizzi, scadenze e retry di polling per in-progress/error.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>{@code pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches}</li>
 *   <li>{@code pn.national-registries.inipec.batch-request-pk-separator=~}</li>
 *   <li>{@code pn.national-registries.enable-inad-massive-api=true}</li>
 *   <li>{@code pn.national-registries.inipec.max-batch-request-size=4}</li>
 *   <li>{@code pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0}</li>
 *   <li>{@code pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0}</li>
 *   <li>{@code pn.national-registries.inad.max-batch-request-size=4}</li>
 *   <li>{@code pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0}</li>
 *   <li>{@code pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0}</li>
 *   <li>{@code pn.national-registries.inad.batch-polling-max-retry=5}</li>
 *   <li>{@code pn.national-registries.inad.batch-polling-in-progress-max-retry=10}</li>
 *   <li>{@code pn.national-registries.inad.polling-error-retry-after=1}</li>
 *   <li>{@code pn.national-registries.inad.polling-in-progress-retry-after=1}</li>
 *   <li>{@code pn.national-registries.inad.oldest-request-max-waiting-seconds=0}</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inipec.max-batch-request-size=4",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.max-batch-request-size=4",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
})
public class DigitalAddressPGMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String IPA_AUTH_ID = "secure_Password123";
    private static final String PG_INAD_FOUND = "75120855137";
    private static final String PG_INAD_NOT_FOUND = "33068456921";
    private static final String PG_INAD_EXPIRED = "49603127470";
    private static final String PG_IPA_INVALID = "32383524660";
    private static final String PG_FOUND = "75120855137";
    private static final String PG_FALLBACK = "33068456921";
    private static final String PG_ER = "49603127470";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @Autowired
    private InipecPollingService inipecPollingService;

    /**
     * Verifica end-to-end il fallback PG da IniPEC a INAD massive fino al risultato finale.
     */
    @Test
    void PG_Digital_FullFlow_InipecFallback_InadMassive() {
        String foundCorrelationId = "pg-massive-found-" + UUID.randomUUID();
        String notFoundCorrelationId = "pg-massive-not-found-" + UUID.randomUUID();
        String expiredCorrelationId = "pg-massive-expired-" + UUID.randomUUID();
        String invalidIpaCorrelationId = "pg-massive-invalid-ipa-" + UUID.randomUUID();

        List<TestRequest> requests = List.of(
                new TestRequest(PG_INAD_FOUND, foundCorrelationId),
                new TestRequest(PG_INAD_NOT_FOUND, notFoundCorrelationId),
                new TestRequest(PG_INAD_EXPIRED, expiredCorrelationId),
                new TestRequest(PG_IPA_INVALID, invalidIpaCorrelationId)
        );

        when(ipaClient.callEServiceWS23(PG_INAD_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_NOT_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_EXPIRED, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_IPA_INVALID, IPA_AUTH_ID)).thenReturn(Mono.just(ipaFoundResponse(PG_IPA_INVALID, "invalid-ipa-pec")));

        for (TestRequest request : requests) {
            AddressOKDto response = enqueueGatewayRequest("PG", request.taxId(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, request.correlationId());
            processNextGatewayInputMessage();

            assertEquals(request.correlationId(), response.getCorrelationId());
            assertInipecPendingRequest(request);
        }

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        ArgumentCaptor<IniPecBatchRequest> inipecRequestCaptor = ArgumentCaptor.forClass(IniPecBatchRequest.class);
        verify(infoCamereClient).callEServiceRequestId(inipecRequestCaptor.capture());
        assertEquals(Set.of(PG_INAD_FOUND, PG_INAD_NOT_FOUND, PG_INAD_EXPIRED, PG_IPA_INVALID), new HashSet<>(inipecRequestCaptor.getValue().getElencoCf().stream().map(IniPecBatchRequest.IniPecCf::getCf).toList()));

        String inipecBatchId = assertSameWorkingBatch(requests, "INIPEC");
        assertEquals("REQUEST_SENT", findBatch(inipecBatchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(inipecBatchId).get("status").s());

        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithState(PG_INAD_FOUND, "ignored-found@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponseWithState(PG_INAD_NOT_FOUND, "ignored-not-found@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PG_INAD_EXPIRED, "ignored-expired@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponse(PG_IPA_INVALID, "invalid-inipec-pec")
        )));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INIPEC, inipecBatchId, inipecPollingId, 4));
        assertEquals("WORKED", findBatch(inipecBatchId).get("status").s());

        for (TestRequest request : requests) {
            Map<String, AttributeValue> inipecRequest = findRequestByRegistry(request.correlationId(), "INIPEC");
            assertEquals("WORKED", inipecRequest.get("status").s());
            assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
            assertNull(inipecRequest.get("resultPayload"));

            Map<String, AttributeValue> inadRequest = findRequestByRegistry(request.correlationId(), "INAD");
            assertEquals("NOT_WORKED", inadRequest.get("status").s());
            assertEquals("INAD#NOT_WORKED", inadRequest.get("registryStatus").s());
            assertNull(inadRequest.get("batchId"));
        }

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress inadBatchResponse = new ResponseRequestListDigitalAddress();
        inadBatchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(inadBatchResponse));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> inadRequestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(inadRequestCaptor.capture());
        assertEquals(Set.of(PG_INAD_FOUND, PG_INAD_NOT_FOUND, PG_INAD_EXPIRED, PG_IPA_INVALID), new HashSet<>(inadRequestCaptor.getValue().getCodiciFiscali()));

        String inadBatchId = assertSameWorkingBatch(requests, "INAD");
        assertEquals("REQUEST_SENT", findBatch(inadBatchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(inadBatchId).get("status").s());

        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(statusResponse));

        ResponseRequestDigitalAddress foundResponse = inadResponse(PG_INAD_FOUND, "inad-found@pec.it", null);
        ResponseRequestDigitalAddress invalidIpaResponse = inadResponse(PG_IPA_INVALID, "inad-after-invalid@pec.it", null);

        ElementDigitalAddress expiredAddress = inadAddress("expired@pec.it", null);
        expiredAddress.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress expiredResponse = inadResponse(PG_INAD_EXPIRED, List.of(expiredAddress));

        ResponseListDigitalAddress massiveResponse = new ResponseListDigitalAddress();
        massiveResponse.setList(List.of(foundResponse, expiredResponse, invalidIpaResponse));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(massiveResponse));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, inadBatchId, inadPollingId, 4));
        assertEquals("WORKED", findBatch(inadBatchId).get("status").s());

        assertInadFinalResult(foundCorrelationId, "inad-found@pec.it");
        assertInadEmptyResult(notFoundCorrelationId);
        assertInadEmptyResult(expiredCorrelationId);
        assertInadFinalResult(invalidIpaCorrelationId, "inad-after-invalid@pec.it");

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(infoCamereClient).callEServiceRequestPec(inipecPollingId);
        verify(inadClient).checkMassiveRequestStatus(inadPollingId);
        verify(inadClient).getMassiveRequestResult(inadPollingId);
        verify(inadClient, never()).callEService(any(), any());
    }

    /**
     * Verifica che risultati INAD massive PG invalidi, multipli o professionali producano risultato vuoto secondo le regole PG.
     */
    @Test
    void PG_Digital_FullFlow_InadMassive_InvalidMultipleAndProfessionalProduceEmptyResult() {
        String invalidCorrelationId = "pg-massive-invalid-inad-" + UUID.randomUUID();
        String multipleCorrelationId = "pg-massive-multiple-inad-" + UUID.randomUUID();
        String professionalCorrelationId = "pg-massive-professional-inad-" + UUID.randomUUID();

        List<TestRequest> requests = List.of(
                new TestRequest(PG_INAD_FOUND, invalidCorrelationId),
                new TestRequest(PG_INAD_NOT_FOUND, multipleCorrelationId),
                new TestRequest(PG_INAD_EXPIRED, professionalCorrelationId)
        );

        when(ipaClient.callEServiceWS23(PG_INAD_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_NOT_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_EXPIRED, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));

        for (TestRequest request : requests) {
            enqueueGatewayRequest("PG", request.taxId(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, request.correlationId());
            processNextGatewayInputMessage();
            assertInipecPendingRequest(request);
        }

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String inipecBatchId = assertSameWorkingBatch(requests, "INIPEC");
        pollingJob.polling();

        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithState(PG_INAD_FOUND, "ignored-1@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PG_INAD_NOT_FOUND, "ignored-2@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PG_INAD_EXPIRED, "ignored-3@pec.it", Pec.StatoImpresaEnum.NF)
        )));
        processPollingEvent(pollingPayload(GatewayDownstreamService.INIPEC, inipecBatchId, inipecPollingId, 3));

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();
        String inadBatchId = assertSameWorkingBatch(requests, "INAD");
        pollingJob.polling();

        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(massiveResponse(
                inadResponse(PG_INAD_FOUND, "not-an-email", null),
                inadResponse(PG_INAD_NOT_FOUND, List.of(
                        inadAddress("first@pec.it", null),
                        inadAddress("second@pec.it", null)
                )),
                inadResponse(PG_INAD_EXPIRED, "professional@pec.it", "INGEGNERE")
        )));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, inadBatchId, inadPollingId, 3));

        assertInadEmptyResult(invalidCorrelationId);
        assertInadEmptyResult(multipleCorrelationId);
        assertInadEmptyResult(professionalCorrelationId);
        assertEquals("WORKED", findBatch(inadBatchId).get("status").s());
    }

    /**
     * Verifica che, tra indirizzi INAD massive validi e scaduti, venga restituito quello valido.
     */
    @Test
    void PG_Digital_FullFlow_InadMassive_ValidAndExpiredReturnsValid() {
        PgInadFlow flow = createSinglePgInadFallback("pg-massive-valid-expired-");
        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(statusResponse));

        ElementDigitalAddress expired = inadAddress("expired@pec.it", null);
        expired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(massiveResponse(
                inadResponse(VALID_PG_PIVA, List.of(inadAddress("valid@pec.it", null), expired))
        )));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 1));

        assertInadFinalResult(flow.correlationId(), "valid@pec.it");
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Verifica che uno stato in-progress nel polling INAD massive programmi il retry.
     */
    @Test
    void PG_Digital_FullFlow_InadMassive_InProgressSchedulesRetry() {
        PgInadFlow flow = createSinglePgInadFallback("pg-massive-in-progress-");
        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.PRESA_IN_CARICO);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(statusResponse));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 1));

        Map<String, AttributeValue> batch = findBatch(flow.batchId());
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("0", batch.get("errorRetryCount").n());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertNotNull(batch.get("attemptAfter"));
        assertEquals("WORKING", findRequestByRegistry(flow.correlationId(), "INAD").get("status").s());
    }

    /**
     * Verifica che un errore di risultato nel polling INAD massive programmi il retry.
     */
    @Test
    void PG_Digital_FullFlow_InadMassive_ResultErrorSchedulesRetry() {
        PgInadFlow flow = createSinglePgInadFallback("pg-massive-result-error-");
        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.error(new IllegalStateException("INAD unavailable")));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 1));

        Map<String, AttributeValue> batch = findBatch(flow.batchId());
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("0", batch.get("inProgressRetryCount").n());
        assertNotNull(batch.get("attemptAfter"));
        assertEquals("WORKING", findRequestByRegistry(flow.correlationId(), "INAD").get("status").s());
    }

    /**
     * Verifica che il replay del polling INIPEC dopo la creazione del fallback PG non generi una seconda richiesta INAD.
     */
    @Test
    void InipecToInad_FallbackCreatedThenSourceCloseFails_ReplayDoesNotDuplicateFallback() {
        String correlationId = "inipec-inad-replay-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), anyString())).thenReturn(Mono.just(ipaNotFoundResponse()));
        enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = requestsByRegistry(correlationId, "INIPEC").getFirst();
        String batchId = inipecRequest.get("batchId").s();
        pollingJob.polling();

        AtomicBoolean failSourceCloseOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            String pk = invocation.getArgument(0);
            GatewayDownstreamService registry = invocation.getArgument(3);
            boolean isFinal = invocation.getArgument(4);
            if (correlationId.equals(pk) && GatewayDownstreamService.INIPEC.equals(registry) && !isFinal && failSourceCloseOnce.getAndSet(false)) {
                return Mono.error(new IllegalStateException("simulated source close failure"));
            }
            return invocation.callRealMethod();
        }).when(nationalRegistriesRequestsRepository).updateStatusToWorked(anyString(), anyString(), nullable(String.class), any(GatewayDownstreamService.class), anyBoolean());

        Pec nf = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(nf));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1);
        assertThrows(RuntimeException.class, () -> inipecPollingService.handleMessage(payload).block());
        assertEquals(1, requestsByRegistry(correlationId, "INAD").size());

        inipecPollingService.handleMessage(payload).block();

        assertEquals(1, requestsByRegistry(correlationId, "INAD").size());
        assertEquals("WORKED", requestsByRegistry(correlationId, "INIPEC").getFirst().get("status").s());
    }

    /**
     * Verifica che nello stesso batch INIPEC un CF trovato venga chiuso, un CF NF generi fallback INAD e un CF ER venga riprocessato da solo al ciclo successivo.
     */
    @Test
    void Inipec_MixedBatch_FoundFallbackAndEr_RetriesOnlyEr() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("mixed-found-" + UUID.randomUUID(), PG_FOUND);
        cases.put("mixed-fallback-" + UUID.randomUUID(), PG_FALLBACK);
        cases.put("mixed-er-" + UUID.randomUUID(), PG_ER);
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            when(ipaClient.callEServiceWS23(eq(entry.getValue()), anyString())).thenReturn(Mono.just(ipaNotFoundResponse()));
            enqueueGatewayRequest("PG", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
        }

        String pollingId = "inipec-mixed-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = request(cases.keySet().iterator().next(), "INIPEC").get("batchId").s();
        pollingJob.polling();

        Pec found = pecResponse(PG_FOUND, "found@pec.it");
        Pec nf = pecResponse(PG_FALLBACK, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        Pec er = pecResponse(PG_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(found, nf, er));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));
        inipecPollingService.handleMessage(payload(GatewayDownstreamService.INIPEC, batchId, pollingId, 3, 0, 0)).block();

        String foundCorrelation = correlationForTaxId(cases, PG_FOUND);
        String fallbackCorrelation = correlationForTaxId(cases, PG_FALLBACK);
        String erCorrelation = correlationForTaxId(cases, PG_ER);
        assertEquals("WORKED", request(foundCorrelation, "INIPEC").get("status").s());
        assertNotNull(request(foundCorrelation, "INIPEC").get("resultPayload"));
        assertEquals("WORKED", request(fallbackCorrelation, "INIPEC").get("status").s());
        assertEquals("NOT_WORKED", request(fallbackCorrelation, "INAD").get("status").s());
        assertEquals("NOT_WORKED", request(erCorrelation, "INIPEC").get("status").s());
        assertEquals("1", request(erCorrelation, "INIPEC").get("retryCount").n());

        String secondPollingId = "inipec-er-second-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(secondPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        ArgumentCaptor<IniPecBatchRequest> captor = ArgumentCaptor.forClass(IniPecBatchRequest.class);
        verify(infoCamereClient, atLeastOnce()).callEServiceRequestId(captor.capture());
        IniPecBatchRequest last = captor.getAllValues().getLast();
        assertEquals(List.of(PG_ER), last.getElencoCf().stream().map(IniPecBatchRequest.IniPecCf::getCf).toList());
    }

    private String correlationForTaxId(Map<String, String> cases, String taxId) {
        return cases.entrySet().stream().filter(e -> taxId.equals(e.getValue())).map(Map.Entry::getKey).findFirst().orElseThrow();
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

    private List<Map<String, AttributeValue>> requestsByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .toList();
    }

    private PgInadFlow createSinglePgInadFallback(String prefix) {
        String correlationId = prefix + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(VALID_PG_PIVA, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String inipecBatchId = assertSameWorkingBatch(List.of(new TestRequest(VALID_PG_PIVA, correlationId)), "INIPEC");
        pollingJob.polling();

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecPollingResponse(pec)));
        processPollingEvent(pollingPayload(GatewayDownstreamService.INIPEC, inipecBatchId, inipecPollingId, 1));
        assertEquals("NOT_WORKED", findRequestByRegistry(correlationId, "INAD").get("status").s());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();
        String inadBatchId = assertSameWorkingBatch(List.of(new TestRequest(VALID_PG_PIVA, correlationId)), "INAD");
        assertEquals("REQUEST_SENT", findBatch(inadBatchId).get("status").s());
        pollingJob.polling();
        assertEquals("WORKING", findBatch(inadBatchId).get("status").s());
        return new PgInadFlow(correlationId, inadBatchId, inadPollingId);
    }

    private ResponseListDigitalAddress massiveResponse(ResponseRequestDigitalAddress... responses) {
        ResponseListDigitalAddress response = new ResponseListDigitalAddress();
        response.setList(List.of(responses));
        return response;
    }

    private void assertInipecPendingRequest(TestRequest request) {
        Map<String, AttributeValue> storedRequest = findRequestByRegistry(request.correlationId(), "INIPEC");
        assertEquals(request.taxId(), storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private String assertSameWorkingBatch(List<TestRequest> requests, String registry) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            String batchId = null;
            boolean ready = true;
            for (TestRequest request : requests) {
                Map<String, AttributeValue> storedRequest = findRequestByRegistry(request.correlationId(), registry);
                AttributeValue status = storedRequest.get("status");
                AttributeValue requestBatchId = storedRequest.get("batchId");
                if (status == null || !"WORKING".equals(status.s()) || requestBatchId == null || requestBatchId.s() == null) {
                    ready = false;
                    break;
                }
                if (batchId == null) {
                    batchId = requestBatchId.s();
                } else if (!batchId.equals(requestBatchId.s())) {
                    fail("Requests have different batchId for registry " + registry);
                }
            }
            if (ready && batchId != null) {
                return batchId;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for " + registry + " batch");
            }
        }

        String batchId = null;
        for (TestRequest request : requests) {
            Map<String, AttributeValue> storedRequest = findRequestByRegistry(request.correlationId(), registry);
            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals(registry + "#WORKING", storedRequest.get("registryStatus").s());
            assertNotNull(storedRequest.get("batchId"));
            if (batchId == null) {
                batchId = storedRequest.get("batchId").s();
            } else {
                assertEquals(batchId, storedRequest.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        return batchId;
    }

    private void assertInadFinalResult(String correlationId, String expectedAddress) {
        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals(expectedAddress, payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void assertInadEmptyResult(String correlationId) {
        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
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

    private void processPollingEvent(PnPollingEvent.Payload payload) {
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(payload).build());
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

    private record PgInadFlow(String correlationId, String batchId, String pollingId) {
    }

    private record TestRequest(String taxId, String correlationId) {
    }
}