package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
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
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.inad.batch-polling-max-retry=2",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inad.polling-error-retry-after=60",
        "pn.national-registries.inad.polling-in-progress-retry-after=30"
})
class InadPollingPFFallbackIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_ONLY_PERSONAL = "PPPPLT80A01H501V";
    private static final String PF_PERSONAL_PROFESSIONAL = "RSSMRA80A01H501U";
    private static final String PF_ONLY_PROFESSIONAL = "VRDLGI80A01H501Q";
    private static final String PF_INVALID_PERSONAL_VALID_PROFESSIONAL = "BNCLCU80A01H501R";
    private static final String PF_VALID_PERSONAL_INVALID_PROFESSIONAL = "FRNGNN80A01H501S";
    private static final String PF_BOTH_INVALID = "MRTPLA80A01H501T";
    private static final String PF_EMPTY_LIST = "RSSGNN80A01H501A";
    private static final String PF_MISSING_FROM_RESPONSE = "BNCMRA80A01H501B";
    private static final String PF_MULTIPLE_PROFESSIONALS = "VRDLGU80A01H501C";
    private static final String PG_SINGLE_VALID = "33068456921";
    private static final String PG_SINGLE_PROFESSIONAL = "92704409926";
    private static final String PG_MULTIPLE = "49603127470";
    private static final String PG_INVALID = "32383524660";
    private static final String PG_EMPTY_LIST = "57894706886";
    private static final String PG_MISSING_FROM_RESPONSE = "12633532093";

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
    void shouldTakePersonalWhenOnlyPersonalExists() {
        String batchId = "batch-only-personal";
        String pollingId = "polling-only-personal";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-only-personal", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_ONLY_PERSONAL, inadAddress("personal@pec.it", null))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-only-personal", "personal@pec.it", "PERSONALE");
    }

    @Test
    void shouldPreferPersonalWhenPersonalAndProfessionalExist() {
        String batchId = "batch-personal-professional";
        String pollingId = "polling-personal-professional";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-personal-professional", PF_PERSONAL_PROFESSIONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_PERSONAL_PROFESSIONAL,
                        inadAddress("personal@pec.it", null),
                        inadAddress("professional@pec.it", "INGEGNERE"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-personal-professional", "personal@pec.it", "PERSONALE");
    }

    @Test
    void shouldTakeProfessionalWhenOnlyProfessionalExists() {
        String batchId = "batch-only-professional";
        String pollingId = "polling-only-professional";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-only-professional", PF_ONLY_PROFESSIONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_ONLY_PROFESSIONAL, inadAddress("professional@pec.it", "INGEGNERE"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-only-professional", "professional@pec.it", "PROFESSIONISTA");
    }

    @Test
    void shouldTakeProfessionalWhenPersonalInvalidAndProfessionalValid() {
        String batchId = "batch-invalid-personal-valid-professional";
        String pollingId = "polling-invalid-personal-valid-professional";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-invalid-personal-valid-professional", PF_INVALID_PERSONAL_VALID_PROFESSIONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_INVALID_PERSONAL_VALID_PROFESSIONAL,
                        inadAddress("invalid-personal", null),
                        inadAddress("professional@pec.it", "INGEGNERE"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-invalid-personal-valid-professional", "professional@pec.it", "PROFESSIONISTA");
    }

    @Test
    void shouldTakePersonalWhenPersonalValidAndProfessionalInvalid() {
        String batchId = "batch-valid-personal-invalid-professional";
        String pollingId = "polling-valid-personal-invalid-professional";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-valid-personal-invalid-professional", PF_VALID_PERSONAL_INVALID_PROFESSIONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_VALID_PERSONAL_INVALID_PROFESSIONAL,
                        inadAddress("personal@pec.it", null),
                        inadAddress("invalid-professional", "INGEGNERE"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-valid-personal-invalid-professional", "personal@pec.it", "PERSONALE");
    }

    @Test
    void shouldFallbackToInipecWhenPersonalAndProfessionalInvalid() {
        String batchId = "batch-both-invalid";
        String pollingId = "polling-both-invalid";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-both-invalid", PF_BOTH_INVALID, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_BOTH_INVALID,
                        inadAddress("invalid-personal", null),
                        inadAddress("invalid-professional", "INGEGNERE"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertInipecFallback(batchId, "corr-both-invalid");
    }

    @Test
    void shouldFallbackToInipecWhenDigitalAddressListIsEmpty() {
        String batchId = "batch-empty-list";
        String pollingId = "polling-empty-list";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-empty-list", PF_EMPTY_LIST, "PF", RequestStatusEnum.WORKING);

        ResponseRequestDigitalAddress response = inadResponse(PF_EMPTY_LIST);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(response)));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertInipecFallback(batchId, "corr-empty-list");
    }

    @Test
    void shouldFallbackToInipecWhenTaxIdIsMissingFromMassiveResponse() {
        String batchId = "batch-missing-tax-id";
        String pollingId = "polling-missing-tax-id";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-missing-tax-id", PF_MISSING_FROM_RESPONSE, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse()));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertInipecFallback(batchId, "corr-missing-tax-id");
    }

    @Test
    void shouldTakeProfessionalWhenMultipleProfessionalsExistAndNoPersonal() {
        String batchId = "batch-multiple-professionals";
        String pollingId = "polling-multiple-professionals";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-multiple-professionals", PF_MULTIPLE_PROFESSIONALS, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(inadPollingResponse(
                inadResponse(PF_MULTIPLE_PROFESSIONALS,
                        inadAddress("prof1@pec.it", "INGEGNERE"),
                        inadAddress("prof2@pec.it", "ARCHITETTO"))
        )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        NationalRegistriesRequest request = findRequest(batchId, "corr-multiple-professionals", GatewayDownstreamService.INAD);
        Assertions.assertEquals(RequestStatusEnum.WORKED, request.getStatus());
        Assertions.assertNotNull(request.getResultPayload());
        Assertions.assertTrue(request.getResultPayload().contains("prof1@pec.it") || request.getResultPayload().contains("prof2@pec.it"));
        Assertions.assertNull(findRequestOrNull(batchId, "corr-multiple-professionals", GatewayDownstreamService.INIPEC));
    }

    @Test
    void shouldScheduleInProgressRetryWithoutTouchingRequests() {
        String batchId = "batch-in-progress";
        String pollingId = "polling-in-progress";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-in-progress", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.IN_ELABORAZIONE)));

        Instant start = Instant.now();
        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        Assertions.assertEquals(0, batch.getErrorRetryCount());
        Assertions.assertEquals(1, batch.getInProgressRetryCount());
        assertAttemptAfter(start, batch.getAttemptAfter(), 30);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-in-progress", GatewayDownstreamService.INAD).getStatus());
    }

    @Test
    void shouldScheduleNormalRetryOnStatusError() {
        String batchId = "batch-status-error";
        String pollingId = "polling-status-error";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-status-error", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.error(pnException("temporary issue", 500)));

        Instant start = Instant.now();
        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        Assertions.assertEquals(1, batch.getErrorRetryCount());
        Assertions.assertEquals(0, batch.getInProgressRetryCount());
        assertAttemptAfter(start, batch.getAttemptAfter(), 60);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-status-error", GatewayDownstreamService.INAD).getStatus());
    }

    @Test
    void shouldScheduleNormalRetryOnResultError() {
        String batchId = "batch-result-error";
        String pollingId = "polling-result-error";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-result-error", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.error(pnException("temporary result issue", 500)));

        Instant start = Instant.now();
        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        Assertions.assertEquals(1, batch.getErrorRetryCount());
        Assertions.assertEquals(0, batch.getInProgressRetryCount());
        assertAttemptAfter(start, batch.getAttemptAfter(), 60);
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-result-error", GatewayDownstreamService.INAD).getStatus());
    }

    @Test
    void shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest() {
        String normalBatchId = "batch-retry-exhausted";
        String inProgressBatchId = "batch-in-progress-exhausted";
        String badRequestBatchId = "batch-bad-request";

        putBatch(normalBatchId, BatchesStatusEnum.WORKING, 1);
        putBatch(inProgressBatchId, BatchesStatusEnum.WORKING, 1);
        putBatch(badRequestBatchId, BatchesStatusEnum.WORKING, 1);

        putRequest(normalBatchId, "corr-retry-exhausted", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);
        putRequest(inProgressBatchId, "corr-in-progress-exhausted", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);
        putRequest(badRequestBatchId, "corr-bad-request", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus("polling-retry-exhausted")).thenReturn(Mono.error(pnException("temporary issue", 500)));
        when(inadClient.checkMassiveRequestStatus("polling-in-progress-exhausted")).thenReturn(Mono.just(status(StatusProcessingRequest.IN_ELABORAZIONE)));
        when(inadClient.checkMassiveRequestStatus("polling-bad-request")).thenReturn(Mono.error(pnException("invalid request", 400)));

        consume(pollingPayload(normalBatchId, "polling-retry-exhausted", 1, 1, 0));
        consume(pollingPayload(inProgressBatchId, "polling-in-progress-exhausted", 1, 0, 1));
        consume(pollingPayload(badRequestBatchId, "polling-bad-request", 1, 0, 0));

        assertBatchAndRequestError(normalBatchId, "corr-retry-exhausted");
        assertBatchAndRequestError(inProgressBatchId, "corr-in-progress-exhausted");
        assertBatchAndRequestError(badRequestBatchId, "corr-bad-request");
    }

    @Test
    void shouldPropagateUnhandledConsumerErrorAndLeaveStateUntouched() {
        String batchId = "batch-unhandled";
        String pollingId = "polling-unhandled";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-unhandled", PF_ONLY_PERSONAL, "PF", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.error(new IllegalStateException("unexpected infrastructure error")));

        Assertions.assertThrows(RuntimeException.class, () -> consume(pollingPayload(batchId, pollingId, 1, 0, 0)));

        NationalRegistriesBatch batch = getBatch(batchId);
        Assertions.assertEquals(BatchesStatusEnum.WORKING, batch.getStatus());
        Assertions.assertEquals(0, batch.getErrorRetryCount());
        Assertions.assertEquals(0, batch.getInProgressRetryCount());
        Assertions.assertEquals(RequestStatusEnum.WORKING, findRequest(batchId, "corr-unhandled", GatewayDownstreamService.INAD).getStatus());
    }

    @Test
    void shouldTakePecForPgWhenSingleAddressWithoutPracticedProfession() {
        String batchId = "batch-pg-single-valid";
        String pollingId = "polling-pg-single-valid";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-single-valid", PG_SINGLE_VALID, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse(
                        inadResponse(PG_SINGLE_VALID, inadAddress("pg@pec.it", null))
                )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadResult(batchId, "corr-pg-single-valid", "pg@pec.it");
    }

    @Test
    void shouldReturnNotFoundForPgWhenSingleAddressHasPracticedProfession() {
        String batchId = "batch-pg-single-professional";
        String pollingId = "polling-pg-single-professional";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-single-professional", PG_SINGLE_PROFESSIONAL, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse(
                        inadResponse(PG_SINGLE_PROFESSIONAL, inadAddress("professional@pec.it", "INGEGNERE"))
                )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadEmptyResult(batchId, "corr-pg-single-professional");
    }

    @Test
    void shouldReturnNotFoundForPgWhenMultipleAddressesExist() {
        String batchId = "batch-pg-multiple";
        String pollingId = "polling-pg-multiple";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-multiple", PG_MULTIPLE, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse(
                        inadResponse(PG_MULTIPLE,
                                inadAddress("pg1@pec.it", null),
                                inadAddress("pg2@pec.it", null))
                )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadEmptyResult(batchId, "corr-pg-multiple");
    }

    @Test
    void shouldReturnNotFoundForPgWhenSingleAddressIsInvalid() {
        String batchId = "batch-pg-invalid";
        String pollingId = "polling-pg-invalid";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-invalid", PG_INVALID, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse(
                        inadResponse(PG_INVALID, inadAddress("invalid-address", null))
                )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadEmptyResult(batchId, "corr-pg-invalid");
    }

    @Test
    void shouldReturnNotFoundForPgWhenAddressListIsEmpty() {
        String batchId = "batch-pg-empty";
        String pollingId = "polling-pg-empty";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-empty", PG_EMPTY_LIST, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse(
                        inadResponse(PG_EMPTY_LIST)
                )));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadEmptyResult(batchId, "corr-pg-empty");
    }

    @Test
    void shouldReturnNotFoundForPgWhenTaxIdIsMissingFromMassiveResponse() {
        String batchId = "batch-pg-missing";
        String pollingId = "polling-pg-missing";
        putBatch(batchId, BatchesStatusEnum.WORKING, 1);
        putRequest(batchId, "corr-pg-missing", PG_MISSING_FROM_RESPONSE, "PG", RequestStatusEnum.WORKING);

        when(inadClient.checkMassiveRequestStatus(pollingId))
                .thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId))
                .thenReturn(Mono.just(inadPollingResponse()));

        consume(pollingPayload(batchId, pollingId, 1, 0, 0));

        assertFinalInadEmptyResult(batchId, "corr-pg-missing");
    }

    private void assertFinalInadEmptyResult(String batchId, String correlationId) {
        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        NationalRegistriesRequest request = findRequest(batchId, correlationId, GatewayDownstreamService.INAD);
        Assertions.assertEquals(RequestStatusEnum.WORKED, request.getStatus());
        Assertions.assertNotNull(request.getResultPayload());
        Assertions.assertTrue(request.getResultPayload().contains("\"digitalAddress\":[]") || request.getResultPayload().contains("\"digitalAddress\":[ ]"));
        Assertions.assertNull(findRequestOrNull(batchId, correlationId, GatewayDownstreamService.INIPEC));
    }

    private void assertFinalInadResult(String batchId, String correlationId, String address) {
        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        NationalRegistriesRequest request =
                findRequest(batchId, correlationId, GatewayDownstreamService.INAD);

        Assertions.assertEquals(RequestStatusEnum.WORKED, request.getStatus());
        Assertions.assertNotNull(request.getResultPayload());
        Assertions.assertTrue(request.getResultPayload().contains(address));

        Assertions.assertNull(
                findRequestOrNull(batchId, correlationId, GatewayDownstreamService.INIPEC)
        );
    }

    private void assertFinalInadResult(String batchId, String correlationId, String address, String recipient) {
        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        NationalRegistriesRequest request = findRequest(batchId, correlationId, GatewayDownstreamService.INAD);
        Assertions.assertEquals(RequestStatusEnum.WORKED, request.getStatus());
        Assertions.assertNotNull(request.getResultPayload());
        Assertions.assertTrue(request.getResultPayload().contains(address));
        Assertions.assertTrue(request.getResultPayload().contains(recipient));
        Assertions.assertNull(findRequestOrNull(batchId, correlationId, GatewayDownstreamService.INIPEC));
    }

    private void assertInipecFallback(String batchId, String correlationId) {
        Assertions.assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        NationalRegistriesRequest inad = findRequest(batchId, correlationId, GatewayDownstreamService.INAD);
        Assertions.assertEquals(RequestStatusEnum.WORKED, inad.getStatus());
        Assertions.assertEquals("INAD#WORKED", inad.getRegistryStatus());
        Assertions.assertNull(inad.getResultPayload());

        NationalRegistriesRequest inipec = findRequest(batchId, correlationId, GatewayDownstreamService.INIPEC);
        Assertions.assertEquals(RequestStatusEnum.NOT_WORKED, inipec.getStatus());
        Assertions.assertEquals("INIPEC#NOT_WORKED", inipec.getRegistryStatus());
        Assertions.assertNull(inipec.getResultPayload());
        Assertions.assertNull(inipec.getBatchId());
    }

    private void assertBatchAndRequestError(String batchId, String correlationId) {
        Assertions.assertEquals(BatchesStatusEnum.ERROR, getBatch(batchId).getStatus());
        Assertions.assertEquals(RequestStatusEnum.ERROR, findRequest(batchId, correlationId, GatewayDownstreamService.INAD).getStatus());
    }

    private void assertAttemptAfter(Instant start, Instant attemptAfter, long expectedSeconds) {
        Assertions.assertNotNull(attemptAfter);
        long seconds = Duration.between(start, attemptAfter).getSeconds();
        Assertions.assertTrue(seconds >= expectedSeconds - 5 && seconds <= expectedSeconds + 5, "Unexpected attemptAfter delta: " + seconds);
    }

    private void consume(PnPollingEvent.Payload payload) {
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(payload).build());
    }

    private NationalRegistriesBatch getBatch(String batchId) {
        NationalRegistriesBatch batch = batchesRepository.getBatch(batchId).block();
        Assertions.assertNotNull(batch);
        return batch;
    }

    private NationalRegistriesRequest findRequest(String batchId, String correlationId, GatewayDownstreamService registry) {
        NationalRegistriesRequest request = findRequestOrNull(batchId, correlationId, registry);
        Assertions.assertNotNull(request, "Missing request " + correlationId + " for registry " + registry);
        return request;
    }

    private NationalRegistriesRequest findRequestOrNull(String batchId, String correlationId, GatewayDownstreamService registry) {
        List<NationalRegistriesRequest> requests = findRequestsByCorrelationId(correlationId);
        NationalRegistriesRequest request = requests.stream().filter(item -> registry == item.getRegistry()).findFirst().orElse(null);

        if (request != null && registry == GatewayDownstreamService.INAD) {
            Assertions.assertEquals(batchId, request.getBatchId());
        }

        if (request != null && registry == GatewayDownstreamService.INIPEC) {
            Assertions.assertNull(request.getBatchId());
        }

        return request;
    }

    private List<NationalRegistriesRequest> findRequestsByCorrelationId(String correlationId) {
        DynamoDbAsyncTable<NationalRegistriesRequest> table = enhancedAsyncClient.table("pn-NationalRegistriesRequests", TableSchema.fromBean(NationalRegistriesRequest.class));
        QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(correlationId).build());
        return Flux.from(table.query(r -> r.queryConditional(query))).flatMapIterable(software.amazon.awssdk.enhanced.dynamodb.model.Page::items).collectList().block();
    }

    private PnPollingEvent.Payload pollingPayload(String batchId, String pollingId, int batchSize, Integer retryCount, Integer inProgressRetryCount) {
        return PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INAD).batchId(batchId).pollingId(pollingId).batchSize(batchSize).retryCount(retryCount).inProgressRetryCount(inProgressRetryCount).build();
    }

    private void putRequest(String batchId, String correlationId, String taxId, String recipientType, RequestStatusEnum status) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        request.setTaxId(taxId);
        request.setRecipientType(recipientType);
        request.setDomicileType("DIGITAL");
        request.setClientId("pn-delivery");
        request.setRegistry(GatewayDownstreamService.INAD);
        request.setStatus(status);
        request.setRegistryStatus(GatewayDownstreamService.INAD.name() + "#" + status.name());
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
        batch.setRegistry(GatewayDownstreamService.INAD.name());
        batch.setStatus(status);
        batch.setPollingId("polling-id");
        batch.setBatchSize(batchSize);
        batch.setAttemptAfter(Instant.now().minusSeconds(10));
        batch.setErrorRetryCount(0);
        batch.setInProgressRetryCount(0);
        batch.setUpdatedAt(Instant.now());
        batch.setTtl(Instant.now().plusSeconds(3600).getEpochSecond());
        batchesRepository.putBatch(batch).block();
    }

    private ResponseStatusListDigitalAddress status(StatusProcessingRequest state) {
        ResponseStatusListDigitalAddress response = new ResponseStatusListDigitalAddress();
        response.setState(state);
        return response;
    }

    private ResponseListDigitalAddress inadPollingResponse(ResponseRequestDigitalAddress... responses) {
        ResponseListDigitalAddress response = new ResponseListDigitalAddress();
        response.setList(List.of(responses));
        return response;
    }

    private ResponseRequestDigitalAddress inadResponse(String taxId, ElementDigitalAddress... addresses) {
        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(taxId);
        response.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        response.setDigitalAddress(List.of(addresses));
        return response;
    }

    protected ElementDigitalAddress inadAddress(String address, String practicedProfession) {
        ElementDigitalAddress element = new ElementDigitalAddress();
        element.setDigitalAddress(address);
        element.setPracticedProfession(practicedProfession);
        return element;
    }

    private PnNationalRegistriesException pnException(String message, int statusCode) {
        return new PnNationalRegistriesException(message, statusCode, "error", null, null, Charset.defaultCharset(), null);
    }
}