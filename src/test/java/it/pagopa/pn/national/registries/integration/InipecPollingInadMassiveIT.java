package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepositoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.dao.requestsTableName=pn-NationalRegistriesRequests",
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inipec.polling-error-retry-after=60",
        "pn.national-registries.inipec.polling-in-progress-retry-after=30",
        "pn.national-registries.inipec.batch-request-max-retry=2",
        "pn.national-registries.inipec.request-retry-after=60",
        "pn.national-registries.inipec.batch-request-pk-separator=~"
})
class InipecPollingInadMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_VALID_PEC = "PPPPLT80A01H501V";
    private static final String PF_INVALID_PEC = "RSSMRA80A01H501U";
    private static final String PF_EMPTY_PEC = "VRDLGI80A01H501Q";
    private static final String PF_ND = "BNCLCU80A01H501R";
    private static final String PF_NF = "FRNGNN80A01H501S";
    private static final String PF_ER = "MRTPLA80A01H501T";
    private static final String PG_VALID_PEC = "33068456921";
    private static final String PG_INVALID_PEC = "92704409926";
    private static final String PG_EMPTY_PEC = "49603127470";
    private static final String PG_ND = "32383524660";
    private static final String PG_NF = "57894706886";
    private static final String PG_ER = "12633532093";
    private static final String PG_INAD_NOT_FOUND = "69318467193";
    private static final String PG_INAD_ERROR = "41003309824";

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    @Autowired
    private NationalRegistriesRequestsRepositoryImpl requestsRepository;

    @Autowired
    private NationalRegistriesBatchesRepository batchesRepository;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedAsyncClient enhancedAsyncClient;

    @BeforeEach
    void setUp() {
        clearTable("pn-NationalRegistriesRequests", "correlationId", "createdAt");
        clearTable("pn-NationalRegistriesBatches", "batchId", "sk");
    }

    @Test
    void shouldHandleAllInipecOutcomesForPf() {
        String batchId = "batch-pf-outcomes";
        String pollingId = "polling-pf-outcomes";
        putBatch(batchId, BatchesStatusEnum.WORKING, 6);
        putRequest(batchId, "corr-pf-valid", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pf-invalid", PF_INVALID_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pf-empty", PF_EMPTY_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pf-nd", PF_ND, "PF", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pf-nf", PF_NF, "PF", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pf-er", PF_ER, "PF", RequestStatusEnum.WORKING);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PF_VALID_PEC, "pf-valid@pec.it"),
                pecResponse(PF_INVALID_PEC, "invalid-pec"),
                pecResponse(PF_EMPTY_PEC, ""),
                pecResponseWithState(PF_ND, "ignored-nd@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponseWithState(PF_NF, "ignored-nf@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PF_ER, "ignored-er@pec.it", Pec.StatoImpresaEnum.ER)
        )));

        Instant erStart = Instant.now();
        consume(pollingPayload(batchId, pollingId, 6, 0, 0));

        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        NationalRegistriesRequest valid = findRequest(batchId, "corr-pf-valid", GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.WORKED, valid.getStatus());
        Assertions.assertTrue(valid.getResultPayload().contains("pf-valid@pec.it"));

        assertFinalInipecEmptyResult(batchId, "corr-pf-invalid");
        assertFinalInipecEmptyResult(batchId, "corr-pf-empty");
        assertFinalInipecEmptyResult(batchId, "corr-pf-nd");
        assertFinalInipecEmptyResult(batchId, "corr-pf-nf");

        NationalRegistriesRequest er = findRequest(batchId, "corr-pf-er", GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.NOT_WORKED, er.getStatus());
        Assertions.assertEquals(1, er.getRetryCount());
        Assertions.assertNull(er.getResultPayload());
        assertAttemptAfter(erStart, Instant.parse(er.getAttemptAfter()), 60);
        Assertions.assertNull(findRequestOrNull(batchId, "corr-pf-er", GatewayDownstreamService.INAD));

        Assertions.assertEquals(6, findRequestsByBatch(batchId).size());
    }

    @Test
    void shouldHandleAllInipecOutcomesForPg() {
        String batchId = "batch-pg-outcomes";
        String pollingId = "polling-pg-outcomes";
        putBatch(batchId, BatchesStatusEnum.WORKING, 8);
        putRequest(batchId, "corr-pg-valid", PG_VALID_PEC, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-invalid", PG_INVALID_PEC, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-empty", PG_EMPTY_PEC, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-nd", PG_ND, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-nf", PG_NF, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-er", PG_ER, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-inad-not-found", PG_INAD_NOT_FOUND, "PG", RequestStatusEnum.WORKING);
        putRequest(batchId, "corr-pg-inad-error", PG_INAD_ERROR, "PG", RequestStatusEnum.WORKING);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PG_VALID_PEC, "pg-valid@pec.it"),
                pecResponse(PG_INVALID_PEC, "invalid-pec"),
                pecResponse(PG_EMPTY_PEC, ""),
                pecResponseWithState(PG_ND, "ignored-nd@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponseWithState(PG_NF, "ignored-nf@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PG_ER, "ignored-er@pec.it", Pec.StatoImpresaEnum.ER),
                pecResponseWithState(PG_INAD_NOT_FOUND, "ignored-inad-not-found@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponseWithState(PG_INAD_ERROR, "ignored-inad-error@pec.it", Pec.StatoImpresaEnum.NF)
        )));

        Instant erStart = Instant.now();
        consume(pollingPayload(batchId, pollingId, 8, 0, 0));

        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        NationalRegistriesRequest valid = findRequest(batchId, "corr-pg-valid", GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.WORKED, valid.getStatus());
        Assertions.assertEquals("INIPEC#WORKED", valid.getRegistryStatus());
        Assertions.assertEquals(batchId, valid.getBatchId());
        Assertions.assertNotNull(valid.getResultPayload());
        Assertions.assertTrue(valid.getResultPayload().contains("pg-valid@pec.it"));
        Assertions.assertNull(findRequestOrNull(batchId, "corr-pg-valid", GatewayDownstreamService.INAD));

        assertMassiveInadFallback(batchId, "corr-pg-invalid");
        assertMassiveInadFallback(batchId, "corr-pg-empty");
        assertMassiveInadFallback(batchId, "corr-pg-nd");
        assertMassiveInadFallback(batchId, "corr-pg-nf");
        assertMassiveInadFallback(batchId, "corr-pg-inad-not-found");
        assertMassiveInadFallback(batchId, "corr-pg-inad-error");

        NationalRegistriesRequest er = findRequest(batchId, "corr-pg-er", GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.NOT_WORKED, er.getStatus());
        Assertions.assertEquals("INIPEC#NOT_WORKED", er.getRegistryStatus());
        Assertions.assertEquals(batchId, er.getBatchId());
        Assertions.assertEquals(1, er.getRetryCount());
        Assertions.assertNull(er.getResultPayload());
        assertAttemptAfter(erStart, Instant.parse(er.getAttemptAfter()), 60);
        Assertions.assertNull(findRequestOrNull(batchId, "corr-pg-er", GatewayDownstreamService.INAD));

        Assertions.assertEquals(8, findRequestsByBatch(batchId).size());
        Assertions.assertEquals(1, findRequestsByCorrelationId("corr-pg-valid").size());
        Assertions.assertEquals(1, findRequestsByCorrelationId("corr-pg-er").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-invalid").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-empty").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-nd").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-nf").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-inad-not-found").size());
        Assertions.assertEquals(2, findRequestsByCorrelationId("corr-pg-inad-error").size());
    }

    @Test
    void shouldScheduleBothPollingRetryKindsWithoutTouchingRequests() {
        String inProgressBatchId = "batch-in-progress";
        String normalRetryBatchId = "batch-normal-retry";
        putBatch(inProgressBatchId, BatchesStatusEnum.WORKING, 1);
        putBatch(normalRetryBatchId, BatchesStatusEnum.WORKING, 1);
        putRequest(inProgressBatchId, "corr-in-progress", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(normalRetryBatchId, "corr-normal-retry", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);

        when(infoCamereClient.callEServiceRequestPec("polling-in-progress")).thenReturn(Mono.error(pnException("List PEC in progress", 404)));
        when(infoCamereClient.callEServiceRequestPec("polling-normal-retry")).thenReturn(Mono.error(pnException("temporary issue", 500)));

        Instant inProgressStart = Instant.now();
        consume(pollingPayload(inProgressBatchId, "polling-in-progress", 1, 0, 0));
        Instant normalRetryStart = Instant.now();
        consume(pollingPayload(normalRetryBatchId, "polling-normal-retry", 1, 0, 0));

        NationalRegistriesBatch inProgressBatch = getBatch(inProgressBatchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, inProgressBatch.getStatus());
        Assertions.assertEquals(0, inProgressBatch.getErrorRetryCount());
        Assertions.assertEquals(1, inProgressBatch.getInProgressRetryCount());
        assertAttemptAfter(inProgressStart, Instant.parse(inProgressBatch.getAttemptAfter()), 30);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(inProgressBatchId, "corr-in-progress", GatewayDownstreamService.INIPEC).getStatus());

        NationalRegistriesBatch normalRetryBatch = getBatch(normalRetryBatchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, normalRetryBatch.getStatus());
        Assertions.assertEquals(1, normalRetryBatch.getErrorRetryCount());
        Assertions.assertEquals(0, normalRetryBatch.getInProgressRetryCount());
        assertAttemptAfter(normalRetryStart, Instant.parse(normalRetryBatch.getAttemptAfter()), 60);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(normalRetryBatchId, "corr-normal-retry", GatewayDownstreamService.INIPEC).getStatus());
    }

    @Test
    void shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest() {
        String normalBatchId = "batch-retry-exhausted";
        String inProgressBatchId = "batch-in-progress-exhausted";
        String badRequestBatchId = "batch-bad-request";
        putBatch(normalBatchId, BatchesStatusEnum.WORKING, 1);
        putBatch(inProgressBatchId, BatchesStatusEnum.WORKING, 1);
        putBatch(badRequestBatchId, BatchesStatusEnum.WORKING, 1);
        putRequest(normalBatchId, "corr-retry-exhausted", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(inProgressBatchId, "corr-in-progress-exhausted", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);
        putRequest(badRequestBatchId, "corr-bad-request", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);

        when(infoCamereClient.callEServiceRequestPec("polling-retry-exhausted")).thenReturn(Mono.error(pnException("temporary issue", 500)));
        when(infoCamereClient.callEServiceRequestPec("polling-in-progress-exhausted")).thenReturn(Mono.error(pnException("List PEC in progress", 404)));
        when(infoCamereClient.callEServiceRequestPec("polling-bad-request")).thenReturn(Mono.error(pnException("invalid request", 400)));

        consume(pollingPayload(normalBatchId, "polling-retry-exhausted", 1, 1, 0));
        consume(pollingPayload(inProgressBatchId, "polling-in-progress-exhausted", 1, 0, 1));
        consume(pollingPayload(badRequestBatchId, "polling-bad-request", 1, 0, 0));

        assertBatchAndRequestError(normalBatchId, "corr-retry-exhausted");
        assertBatchAndRequestError(inProgressBatchId, "corr-in-progress-exhausted");
        assertBatchAndRequestError(badRequestBatchId, "corr-bad-request");
    }

    @Test
    void shouldTreatInfoCamereErrorMetadataAsNormalPollingRetry() {
        String batchId = "batch-error-metadata";
        String pollingId = "polling-error-metadata";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-error-metadata", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);

        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setCode("123");
        response.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        Assertions.assertEquals(1, batch.getErrorRetryCount());
        Assertions.assertEquals(0, batch.getInProgressRetryCount());
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-error-metadata", GatewayDownstreamService.INIPEC).getStatus());
    }

    @Test
    void shouldTreatUnhandledConsumerErrorAsNormalPollingRetry() {
        String batchId = "batch-unhandled";
        String pollingId = "polling-unhandled";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-unhandled", PF_VALID_PEC, "PF", RequestStatusEnum.WORKING);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(new IllegalStateException("unexpected infrastructure error")));

        Instant retryStart = Instant.now();
        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        Assertions.assertEquals(1, batch.getErrorRetryCount());
        Assertions.assertEquals(0, batch.getInProgressRetryCount());
        assertAttemptAfter(retryStart, Instant.parse(batch.getAttemptAfter()), 60);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-unhandled", GatewayDownstreamService.INIPEC).getStatus());
    }

    @Test
    void shouldHandleMissingTaxIdFromInipecResponse() {
        String batchId = "batch-missing-tax-id";
        String pollingId = "polling-missing-tax-id";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-missing-tax-id", PG_VALID_PEC, "PG", RequestStatusEnum.WORKING);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse()));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        assertMassiveInadFallback(batchId, "corr-missing-tax-id");
    }

    private void assertFinalInipecEmptyResult(String batchId, String correlationId) {
        NationalRegistriesRequest request = findRequest(batchId, correlationId, GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.WORKED, request.getStatus());
        assertEmptyDigitalAddressPayload(request.getResultPayload());
        Assertions.assertNull(findRequestOrNull(batchId, correlationId, GatewayDownstreamService.INAD));
    }

    private void assertMassiveInadFallback(String batchId, String correlationId) {
        NationalRegistriesRequest inipec = findRequest(batchId, correlationId, GatewayDownstreamService.INIPEC);
        NationalRegistriesRequest inad = findRequest(batchId, correlationId, GatewayDownstreamService.INAD);
        assertInipecDelegatedToInad(inipec, batchId);
        assertMassiveInadEntity(inad, inipec);
    }

    private void assertInipecDelegatedToInad(NationalRegistriesRequest inipec, String batchId) {
        Assertions.assertEquals(GatewayDownstreamService.INIPEC, inipec.getRegistry());
        Assertions.assertEquals(RequestStatusEnum.WORKED, inipec.getStatus());
        Assertions.assertEquals("INIPEC#WORKED", inipec.getRegistryStatus());
        Assertions.assertEquals(batchId, inipec.getBatchId());
        Assertions.assertNull(inipec.getResultPayload());
    }

    private void assertMassiveInadEntity(NationalRegistriesRequest inad, NationalRegistriesRequest inipec) {
        Assertions.assertNotNull(inad);
        Assertions.assertEquals(inipec.getCorrelationId(), inad.getCorrelationId());
        Assertions.assertEquals(inipec.getTaxId(), inad.getTaxId());
        Assertions.assertEquals(inipec.getRecipientType(), inad.getRecipientType());
        Assertions.assertEquals(inipec.getDomicileType(), inad.getDomicileType());
        Assertions.assertEquals(inipec.getClientId(), inad.getClientId());
        Assertions.assertEquals(inipec.getReferenceRequestDate(), inad.getReferenceRequestDate());
        Assertions.assertNull(inad.getBatchId());
        Assertions.assertEquals(GatewayDownstreamService.INAD, inad.getRegistry());
        Assertions.assertEquals(RequestStatusEnum.NOT_WORKED, inad.getStatus());
        Assertions.assertEquals("INAD#NOT_WORKED", inad.getRegistryStatus());
        Assertions.assertNotNull(inad.getCreatedAt());
        Assertions.assertFalse(inad.getCreatedAt().isBefore(inipec.getCreatedAt()));
        Assertions.assertNull(inad.getResultPayload());
        Assertions.assertNull(inad.getUpdatedAt());
        Assertions.assertNull(inad.getRetryCount());
        Assertions.assertNull(inad.getTtl());
    }

    private void assertBatchAndRequestError(String batchId, String correlationId) {
        Assertions.assertEquals(BatchesStatusEnum.ERROR, getBatch(batchId).getStatus());
        Assertions.assertEquals(RequestStatusEnum.ERROR, findRequest(batchId, correlationId, GatewayDownstreamService.INIPEC).getStatus());
    }

    private void assertAttemptAfter(Instant start, Instant attemptAfter, long expectedSeconds) {
        Assertions.assertNotNull(attemptAfter);
        long seconds = Duration.between(start, attemptAfter).getSeconds();
        Assertions.assertTrue(seconds >= expectedSeconds - 5 && seconds <= expectedSeconds + 5, "Unexpected attemptAfter delta: " + seconds);
    }

    private void assertEmptyDigitalAddressPayload(String resultPayload) {
        Assertions.assertNotNull(resultPayload);
        Assertions.assertTrue(resultPayload.contains("\"digitalAddress\":[]") || resultPayload.contains("\"digitalAddress\":[ ]"));
    }

    private void consume(PnPollingEvent.Payload payload) {
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(payload).build());
    }

    private NationalRegistriesBatch getBatch(String batchId) {
        NationalRegistriesBatch batch = batchesRepository.getBatch(batchId).block();
        Assertions.assertNotNull(batch);
        return batch;
    }

    private List<NationalRegistriesRequest> findRequestsByBatch(String batchId) {
        return Flux.from(requestsRepository.getRequestsBatchId(batchId, null)).flatMapIterable(Page::items).collectList().block();
    }

    private NationalRegistriesRequest findRequest(String batchId, String correlationId, GatewayDownstreamService registry) {
        NationalRegistriesRequest request = findRequestOrNull(batchId, correlationId, registry);
        Assertions.assertNotNull(request, "Missing request " + correlationId + " for registry " + registry);
        return request;
    }

    private NationalRegistriesRequest findRequestOrNull(String batchId, String correlationId, GatewayDownstreamService registry) {
        return findRequestsByCorrelationId(correlationId).stream().filter(item -> registry == item.getRegistry()).findFirst().orElse(null);
    }

    private List<NationalRegistriesRequest> findRequestsByCorrelationId(String correlationId) {
        DynamoDbAsyncTable<NationalRegistriesRequest> table = enhancedAsyncClient.table("pn-NationalRegistriesRequests", TableSchema.fromBean(NationalRegistriesRequest.class));
        QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(correlationId).build());
        return Flux.from(table.query(r -> r.queryConditional(query))).flatMapIterable(Page::items).collectList().block();
    }

    private PnPollingEvent.Payload pollingPayload(String batchId, String pollingId, int batchSize, Integer retryCount, Integer inProgressRetryCount) {
        return PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INIPEC).batchId(batchId).pollingId(pollingId).batchSize(batchSize).errorRetryCount(retryCount).inProgressRetryCount(inProgressRetryCount).build();
    }

    private void putRequest(String batchId, String correlationId, String taxId, String recipientType, RequestStatusEnum status) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        request.setTaxId(taxId);
        request.setRecipientType(recipientType);
        request.setDomicileType("DIGITAL");
        request.setClientId("pn-delivery");
        request.setRegistry(GatewayDownstreamService.INIPEC);
        request.setStatus(status);
        request.setRegistryStatus(GatewayDownstreamService.INIPEC.name() + "#" + status.name());
        request.setBatchId(batchId);
        request.setReferenceRequestDate("2026-10-10T10:00:00Z");
        request.setUpdatedAt(request.getCreatedAt());
        request.setRetryCount(0);
        request.setTtl(request.getCreatedAt().plusSeconds(24 * 3600).getEpochSecond());
        requestsRepository.putRequest(request).block();
    }

    private void putBatch(String batchId, BatchesStatusEnum status, int batchSize) {
        NationalRegistriesBatch batch = new NationalRegistriesBatch();
        batch.setBatchId(batchId);
        batch.setSk("-");
        batch.setCreatedAt(Instant.now().minusSeconds(60));
        batch.setRegistry(GatewayDownstreamService.INIPEC.name());
        batch.setStatus(status);
        batch.setPollingId("polling-id");
        batch.setBatchSize(batchSize);
        batch.setAttemptAfter(formatInstant(Instant.now().minusSeconds(10)));
        batch.setErrorRetryCount(0);
        batch.setInProgressRetryCount(0);
        batch.setUpdatedAt(Instant.now());
        batch.setTtl(Instant.now().plusSeconds(3600).getEpochSecond());
        batchesRepository.putBatch(batch).block();
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

    private ResponseRequestDigitalAddress inadResponse(String taxId, String digitalAddress) {
        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(taxId);
        response.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        ElementDigitalAddress element = new ElementDigitalAddress();
        element.setDigitalAddress(digitalAddress);
        element.setPracticedProfession("CONSULENTE");
        response.setDigitalAddress(List.of(element));
        return response;
    }

    private PnNationalRegistriesException pnException(String message, int statusCode) {
        return new PnNationalRegistriesException(message, statusCode, "error", null, null, Charset.defaultCharset(), null);
    }
}