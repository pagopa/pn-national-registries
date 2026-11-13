package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.BaseTest;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import reactor.test.StepVerifier;
import org.junit.jupiter.api.AfterEach;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;

@Slf4j
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class,
        properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "pn.national-registries.query-limit=5"
})
class NationalRegistriesRequestsRepositoryIT extends BaseTest.WithLocalStack {

    @MockitoBean
    private SqsService sqsService;

    @MockitoBean
    CheckCfClient checkCfClient;

    @MockitoBean
    VerificheApi verificheApi;

    @MockitoBean
    AdELegalClient adELegalClient;

    @MockitoBean
    AnprClient anprClient;

    @MockitoBean
    InadClient inadClient;

    @MockitoBean
    InfoCamereClient infoCamereClient;

    @Autowired
    NationalRegistriesRequestsRepositoryImpl repository;

    @Autowired
    NationalRegistriesConfig nationalRegistriesConfig;

    private final Set<Map<String, AttributeValue>> fallbackTestKeys = new HashSet<>();

    @AfterEach
    void cleanFallbackTestRequests() {
        fallbackTestKeys.forEach(key -> dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(nationalRegistriesConfig.getDao().getRequestsTableName()).key(key).build()));
    }

    private Map<String, AttributeValue> fallbackTestKey(NationalRegistriesRequest request) {
        var key = Map.of("correlationId", AttributeValue.builder().s(request.getCorrelationId()).build(),
                "createdAt", AttributeValue.builder().s(request.getCreatedAt().toString()).build());
        fallbackTestKeys.add(key);
        return key;
    }

    @Autowired
    DynamoDbClient dynamoDbClient;

    @Test
    void fallbackAndSourceCompletionAreAtomicAndDeferred() {
        for (RequestStatusEnum destinationStatus : List.of(RequestStatusEnum.NOT_WORKED, RequestStatusEnum.WORKED, RequestStatusEnum.ERROR)) {
            NationalRegistriesRequest source = buildRequest("atomic-" + UUID.randomUUID(), Instant.now(),
                    "source-batch", GatewayDownstreamService.INIPEC, RequestStatusEnum.WORKING);
            fallbackTestKey(source);
            repository.putRequest(source).block();
            NationalRegistriesRequest destination = new NationalRegistriesRequest(source, destinationStatus, GatewayDownstreamService.INAD,
                    destinationStatus == RequestStatusEnum.WORKED ? "payload" : null, destinationStatus == RequestStatusEnum.WORKED ? "originalPayload" : null, 10);
            fallbackTestKey(destination);
            var handoff = repository.completeWithFallback(source, destination);
            Assertions.assertEquals(RequestStatusEnum.WORKING, readStoredRequest(source).getStatus());
            Assertions.assertNull(readStoredRequest(destination));

            handoff.block();
            Assertions.assertEquals(RequestStatusEnum.WORKED, readStoredRequest(source).getStatus());
            Assertions.assertEquals("INIPEC#WORKED", readStoredRequest(source).getRegistryStatus());
            Assertions.assertEquals(destinationStatus, readStoredRequest(destination).getStatus());

            // A stale replay cannot create a second destination after the transaction committed.
            NationalRegistriesRequest duplicate = new NationalRegistriesRequest(source, destinationStatus, GatewayDownstreamService.INAD, null, null, 10);
            StepVerifier.create(repository.completeWithFallback(source, duplicate))
                    .expectError(TransactionCanceledException.class).verify();
            Assertions.assertNull(readStoredRequest(duplicate));
            Assertions.assertEquals(destinationStatus, readStoredRequest(destination).getStatus());
        }
    }

    @Test
    void failedFallbackWriteLeavesSourceWorkingAndCanBeRetried() {
        NationalRegistriesRequest source = buildRequest("rollback-" + UUID.randomUUID(), Instant.now(),
                "source-batch", GatewayDownstreamService.INAD, RequestStatusEnum.WORKING);
        fallbackTestKey(source);
        repository.putRequest(source).block();
        NationalRegistriesRequest occupied = new NationalRegistriesRequest(source, RequestStatusEnum.WORKED, GatewayDownstreamService.INIPEC, "existing", "existing", 10);
        fallbackTestKey(occupied);
        repository.putRequest(occupied).block();
        NationalRegistriesRequest destination = new NationalRegistriesRequest(source, RequestStatusEnum.NOT_WORKED, GatewayDownstreamService.INIPEC, null, null, 10);
        destination.setCreatedAt(occupied.getCreatedAt());

        StepVerifier.create(repository.completeWithFallback(source, destination))
                .expectError(TransactionCanceledException.class).verify();
        Assertions.assertEquals(RequestStatusEnum.WORKING, readStoredRequest(source).getStatus());
        Assertions.assertEquals("existing", readStoredRequest(occupied).getResultPayload());

        destination.setCreatedAt(occupied.getCreatedAt().plusNanos(1));
        fallbackTestKey(destination);
        repository.completeWithFallback(source, destination).block();
        Assertions.assertEquals(RequestStatusEnum.WORKED, readStoredRequest(source).getStatus());
        Assertions.assertEquals(RequestStatusEnum.NOT_WORKED, readStoredRequest(destination).getStatus());
    }

    @Test
    void requestCompletionIsDeferredUntilSubscription() {
        NationalRegistriesRequest source = buildRequest("lazy-" + UUID.randomUUID(), Instant.now(),
                "source-batch", GatewayDownstreamService.INAD, RequestStatusEnum.WORKING);
        fallbackTestKey(source);
        repository.putRequest(source).block();
        var completion = repository.updateStatusToWorked(source.getCorrelationId(), source.getCreatedAt().toString(), null, null, source.getRegistry(), null, false);
        NationalRegistriesRequest stored = readStoredRequest(source);
        Assertions.assertEquals(RequestStatusEnum.WORKING, stored.getStatus());
        completion.block();
        NationalRegistriesRequest storedAfterUpdate = readStoredRequest(source);
        Assertions.assertEquals(RequestStatusEnum.WORKED, storedAfterUpdate.getStatus());
        Assertions.assertNotNull(storedAfterUpdate.getTtl());
    }

    private NationalRegistriesRequest readStoredRequest(NationalRegistriesRequest request) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(nationalRegistriesConfig.getDao().getRequestsTableName()).consistentRead(true)
                .key(fallbackTestKey(request)).build());
        return NationalRegistriesRequest.attributeValueMapToNationalRegistriesRequest(response.item());
    }

    @Test
    void putAndGetRequestsByBatchIdTest() {

        String batchId = UUID.randomUUID().toString();
        int numberOfItems = 7;

        Instant baseTime = Instant.parse("2026-10-07T10:00:00Z");

        List<NationalRegistriesRequest> requests = new java.util.ArrayList<>(IntStream.range(0, numberOfItems)
                .mapToObj(i -> buildRequest(
                        "CORRELATION-" + UUID.randomUUID(),
                        baseTime.plus(i, ChronoUnit.SECONDS),
                        batchId,
                        GatewayDownstreamService.INAD,
                        RequestStatusEnum.WORKING
                ))
                .toList());

        requests.add(buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plus(numberOfItems, ChronoUnit.SECONDS),
                batchId,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKED
        ));

        requests.forEach(request -> repository.putRequest(request).block());

        List<Page<NationalRegistriesRequest>> pages = Flux.from(repository.getRequestsBatchId(batchId, RequestStatusEnum.WORKING))
                .collectList()
                .block();

        Assertions.assertNotNull(pages);
        Assertions.assertEquals(2, pages.size());
        Page<NationalRegistriesRequest> firstPage = pages.getFirst();
        Assertions.assertEquals(5, firstPage.items().size());
        Assertions.assertTrue(firstPage.items().stream().allMatch(item -> batchId.equals(item.getBatchId())));
        Assertions.assertFalse(firstPage.lastEvaluatedKey().isEmpty());

        Page<NationalRegistriesRequest> secondPage = pages.get(1);
        Assertions.assertEquals(2, secondPage.items().size());
        Assertions.assertTrue(secondPage.items().stream().allMatch(item -> batchId.equals(item.getBatchId())));
        Assertions.assertTrue(secondPage.lastEvaluatedKey() == null || secondPage.lastEvaluatedKey().isEmpty());
    }

    @Test
    void getRequestsByRegistryStatusTest() {

        Instant baseTime = Instant.parse("2026-10-07T11:00:00Z");

        NationalRegistriesRequest inipecNotWorked = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime,
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inipecNotWorked1 = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(1),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inadNotWorked = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(2),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inipecNotWorked2 = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(3),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inipecNotWorked3 = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                Instant.parse("2099-10-07T11:00:03Z"),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inipecWorked = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                Instant.parse("2099-10-07T11:00:03Z"),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.WORKED
        );

        List.of(inipecNotWorked, inipecNotWorked1, inipecNotWorked2, inipecNotWorked3, inadNotWorked, inipecWorked).forEach(request -> repository.putRequest(request).block());

        Page<NationalRegistriesRequest> result = Flux.from(repository
                        .getRequestsByRegistryStatus(GatewayDownstreamService.INIPEC, RequestStatusEnum.NOT_WORKED, 10))
                .single()
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.items().size());
        String expectedRegistryStatus = GatewayDownstreamService.INIPEC.name() + "#" + RequestStatusEnum.NOT_WORKED.name();
        Assertions.assertTrue(result.items().stream().allMatch(request -> expectedRegistryStatus.equals(request.getRegistryStatus())));
        Assertions.assertTrue(result.items().stream().anyMatch(request -> inipecNotWorked1.getCorrelationId().equals(request.getCorrelationId())));
        Assertions.assertTrue(result.items().stream().anyMatch(request -> inipecNotWorked.getCorrelationId().equals(request.getCorrelationId())));
    }

    @Test
    void updateStatusToWorkingTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        repository.putRequest(request).block();

        String batchId = UUID.randomUUID().toString();

        repository.updateStatusToWorking(request.getCorrelationId(), request.getCreatedAt().toString(), batchId, GatewayDownstreamService.INAD).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId, RequestStatusEnum.WORKING);
        Assertions.assertEquals(RequestStatusEnum.WORKING, updated.getStatus());
        Assertions.assertEquals(batchId, updated.getBatchId());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getUpdatedAt().isAfter(createdAt));
        Assertions.assertNull(updated.getTtl());
    }

    @Test
    void updateStatusToWorkingInvalidStatusTransitionTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.ERROR
        );

        repository.putRequest(request).block();

        String batchId = UUID.randomUUID().toString();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorking(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        batchId,
                        GatewayDownstreamService.INAD
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKING"));
    }

    @Test
    void updateStatusToWorkingInvalidStatusTransition2Test() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKED
        );

        repository.putRequest(request).block();

        String batchId = UUID.randomUUID().toString();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorking(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        batchId,
                        GatewayDownstreamService.INAD
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKING"));
    }

    @Test
    void updateStatusToWorkedNotFinalRegistryTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:10:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.WORKING
        );

        request.setTtl(null);
        repository.putRequest(request).block();

        repository.updateStatusToWorked(
                request.getCorrelationId(),
                request.getCreatedAt().toString(),
                null,
                null,
                GatewayDownstreamService.INIPEC,
                GatewayDownstreamService.INAD,
                false
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId, RequestStatusEnum.WORKED);
        Assertions.assertEquals(RequestStatusEnum.WORKED, updated.getStatus());
        Assertions.assertEquals(GatewayDownstreamService.INIPEC, updated.getRegistry());
        Assertions.assertEquals(GatewayDownstreamService.INAD, updated.getNextRegistry());
        Assertions.assertEquals(GatewayDownstreamService.INIPEC.name() + "#" + RequestStatusEnum.WORKED.name(), updated.getRegistryStatus());
        Assertions.assertNotNull(updated.getTtl());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getTtl() > Instant.now().getEpochSecond());
    }


    @Test
    void updateStatusToWorkedTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:10:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.WORKING
        );

        request.setTtl(null);
        repository.putRequest(request).block();
        String resultPayload = "{\"digitalAddress\":\"test@pec.it\"}";
        String originalPayload = "{\"digitalAddress\":\"test@pec.it\"}";

        repository.updateStatusToWorked(
                request.getCorrelationId(),
                request.getCreatedAt().toString(),
                resultPayload,
                originalPayload,
                GatewayDownstreamService.INIPEC,
                null,
                true
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId, RequestStatusEnum.WORKED);
        Assertions.assertEquals(RequestStatusEnum.WORKED, updated.getStatus());
        Assertions.assertEquals(resultPayload, updated.getResultPayload());
        Assertions.assertEquals(GatewayDownstreamService.INIPEC, updated.getRegistry());
        Assertions.assertEquals(GatewayDownstreamService.INIPEC.name() + "#" + RequestStatusEnum.WORKED.name(), updated.getRegistryStatus());
        Assertions.assertNotNull(updated.getTtl());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getTtl() > Instant.now().getEpochSecond());
    }

    @Test
    void updateStatusToWorkedWithDifferentRegistryTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:10:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.WORKING
        );

        request.setTtl(null);
        repository.putRequest(request).block();
        String resultPayload = "{\"digitalAddress\":\"test@pec.it\"}";
        String originalPayload = "{\"digitalAddress\":\"test@pec.it\"}";

        repository.updateStatusToWorked(
                request.getCorrelationId(),
                request.getCreatedAt().toString(),
                resultPayload,
                originalPayload,
                GatewayDownstreamService.INAD,
                null,
                true
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId, RequestStatusEnum.WORKED);
        Assertions.assertEquals(RequestStatusEnum.WORKED, updated.getStatus());
        Assertions.assertEquals(GatewayDownstreamService.INAD, updated.getRegistry());
        Assertions.assertEquals(GatewayDownstreamService.INAD.name() + "#" + RequestStatusEnum.WORKED.name(), updated.getRegistryStatus());
        Assertions.assertEquals(resultPayload, updated.getResultPayload());
        Assertions.assertNotNull(updated.getTtl());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getTtl() > Instant.now().getEpochSecond());
    }

    @Test
    void updateStatusToWorkedInvalidStatusTransitionTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorked(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        "payload",
                        "payload",
                        null,
                        null,
                        true
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKED"));
    }

    @Test
    void updateStatusToWorkedInvalidStatusTransition2Test() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorked(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        "resultPayload",
                        "originalpayload",
                        null,
                        null,
                        true
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKED"));
    }

    @Test
    void updateStatusToErrorTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:20:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKING
        );

        request.setTtl(null);

        repository.putRequest(request).block();

        repository.updateStatusToError(
                request.getCorrelationId(),
                request.getCreatedAt().toString(),
                GatewayDownstreamService.INAD,
                null
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId, RequestStatusEnum.ERROR);
        Assertions.assertEquals(RequestStatusEnum.ERROR, updated.getStatus());
        Assertions.assertNotNull(updated.getTtl());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getTtl() > Instant.now().getEpochSecond());
    }

    @Test
    void updateStatusToErrorInvalidStatusTransitionTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToError(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        GatewayDownstreamService.INAD,
                        null
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to ERROR"));
    }

    @Test
    void updateStatusToErrorInvalidStatusTransition2Test() {

        Instant createdAt = Instant.parse("2026-10-07T12:00:00Z");
        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToError(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        GatewayDownstreamService.INAD,
                        null
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to ERROR"));
    }


    @Test
    void updateStatusToWorkingWithoutBatchIdShouldThrowException() {

        Instant createdAt = Instant.parse("2026-10-07T12:30:00Z");

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorking(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        null,
                        GatewayDownstreamService.INAD
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("BatchId is required"));
    }

    @Test
    void updateStatusToWorkingWithBlankBatchIdShouldThrowException() {

        Instant createdAt = Instant.parse("2026-10-07T12:31:00Z");

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                null,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorking(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        " ",
                        GatewayDownstreamService.INAD
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("BatchId is required"));
    }

    @Test
    void updateStatusToWorkedWithoutResultPayloadShouldThrowException() {

        Instant createdAt = Instant.parse("2026-10-07T12:40:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKING
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorked(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        null,
                        null,
                        null,
                        null,
                        true
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Result payload is required for final registry"));
    }

    @Test
    void updateStatusToWorkedWithBlankResultPayloadShouldThrowException() {

        Instant createdAt = Instant.parse("2026-10-07T12:41:00Z");
        String batchId = UUID.randomUUID().toString();

        NationalRegistriesRequest request = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                createdAt,
                batchId,
                GatewayDownstreamService.INAD,
                RequestStatusEnum.WORKING
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToWorked(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString(),
                        " ",
                        " ",
                        null,
                        null,
                        true
                ).block()
        );

        Assertions.assertNotNull(exception.getProblem().getDetail());
        Assertions.assertTrue(exception.getProblem().getDetail().contains("Result payload is required for final registry"));
    }

    private NationalRegistriesRequest getRequestByBatchId(String batchId, RequestStatusEnum status ) {
        Page<NationalRegistriesRequest> page = Flux.from(repository.getRequestsBatchId(batchId, status)).next().block();
        Assertions.assertNotNull(page);
        Assertions.assertEquals(1, page.items().size());
        return page.items().getFirst();
    }

    private NationalRegistriesRequest buildRequest(String correlationId, Instant createdAt, String batchId, GatewayDownstreamService registry, RequestStatusEnum status) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setCreatedAt(createdAt);
        request.setAttemptAfter(formatInstant(createdAt));
        request.setTaxId("RSSMRA80A01H501U");
        request.setRecipientType("PF");
        request.setClientId("pn-delivery");
        request.setRegistry(registry);
        request.setStatus(status);
        request.setRegistryStatus(registry.name() + "#" + status.name());
        request.setBatchId(batchId);
        request.setReferenceRequestDate("2026-10-07T10:00:00Z");
        request.setUpdatedAt(createdAt);
        return request;
    }
}