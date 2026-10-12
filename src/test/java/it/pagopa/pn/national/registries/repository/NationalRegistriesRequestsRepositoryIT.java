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
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Slf4j
@SpringBootTest(properties = {
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

    @Test
    void putAndGetRequestsByBatchIdTest() {

        String batchId = UUID.randomUUID().toString();
        int numberOfItems = 7;

        Instant baseTime = Instant.parse("2026-10-07T10:00:00Z");

        List<NationalRegistriesRequest> requests = IntStream.range(0, numberOfItems)
                .mapToObj(i -> buildRequest(
                        "CORRELATION-" + UUID.randomUUID(),
                        baseTime.plus(i, ChronoUnit.SECONDS),
                        batchId,
                        GatewayDownstreamService.INAD,
                        RequestStatusEnum.NOT_WORKED
                ))
                .toList();

        requests.forEach(request -> repository.putRequest(request).block());

        Page<NationalRegistriesRequest> firstPage = repository.getRequestsBatchId(batchId, null).block();

        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(5, firstPage.items().size());
        Assertions.assertTrue(firstPage.items().stream().allMatch(item -> batchId.equals(item.getBatchId())));
        Assertions.assertFalse(firstPage.lastEvaluatedKey().isEmpty());

        Page<NationalRegistriesRequest> secondPage = repository.getRequestsBatchId(batchId, firstPage.lastEvaluatedKey()).block();

        Assertions.assertNotNull(secondPage);
        Assertions.assertEquals(2, secondPage.items().size());
        Assertions.assertTrue(secondPage.items().stream().allMatch(item -> batchId.equals(item.getBatchId())));
        Assertions.assertTrue(secondPage.lastEvaluatedKey() == null || secondPage.lastEvaluatedKey().isEmpty());
    }

    @Test
    void getRequestsByRegistryStatusTest() {

        Instant baseTime = Instant.parse("2026-10-07T11:00:00Z");

        NationalRegistriesRequest inadNotWorked1 = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime,
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inadNotWorked2 = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(1),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inipecNotWorked = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(2),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INAD,
                RequestStatusEnum.NOT_WORKED
        );

        NationalRegistriesRequest inadWorked = buildRequest(
                "CORRELATION-" + UUID.randomUUID(),
                baseTime.plusSeconds(3),
                UUID.randomUUID().toString(),
                GatewayDownstreamService.INIPEC,
                RequestStatusEnum.WORKED
        );

        List.of(inadNotWorked1, inadNotWorked2, inipecNotWorked, inadWorked).forEach(request -> repository.putRequest(request).block());

        Page<NationalRegistriesRequest> result = repository.getRequestsByRegistryStatus(GatewayDownstreamService.INIPEC, null, 10).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.items().size());
        String expectedRegistryStatus = GatewayDownstreamService.INIPEC.name() + "#" + RequestStatusEnum.NOT_WORKED.name();
        Assertions.assertTrue(result.items().stream().allMatch(request -> expectedRegistryStatus.equals(request.getRegistryStatus())));
        Assertions.assertTrue(result.items().stream().anyMatch(request -> inadNotWorked1.getCorrelationId().equals(request.getCorrelationId())));
        Assertions.assertTrue(result.items().stream().anyMatch(request -> inadNotWorked2.getCorrelationId().equals(request.getCorrelationId())));
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

        repository.updateStatusToWorking(request.getCorrelationId(), request.getCreatedAt().toString(), batchId).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId);
        Assertions.assertEquals(RequestStatusEnum.WORKING, updated.getStatus());
        Assertions.assertEquals(batchId, updated.getBatchId());
        Assertions.assertNotNull(updated.getUpdatedAt());
        Assertions.assertTrue(updated.getUpdatedAt().isAfter(createdAt));
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
                        batchId
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKING"));
        Assertions.assertTrue(exception.getProblem().getDetail().contains("the current status is not NOT_WORKED"));
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
                        batchId
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKING"));

        Assertions.assertTrue(
                exception.getProblem().getDetail().contains(
                        "the current status is not NOT_WORKED"
                )
        );
    }


    @Test
    void updateStatusToWorkedTest() {

        Instant createdAt = Instant.parse("2026-10-07T12:10:00Z");
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
        String resultPayload = "{\"digitalAddress\":\"test@pec.it\"}";

        repository.updateStatusToWorked(
                request.getCorrelationId(),
                request.getCreatedAt().toString(),
                resultPayload
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId);
        Assertions.assertEquals(RequestStatusEnum.WORKED, updated.getStatus());
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
                        "payload"
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKED"));
        Assertions.assertTrue(exception.getProblem().getDetail().contains("the current status is not WORKING"));
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
                        "resultPayload"
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to WORKED"));
        Assertions.assertTrue(exception.getProblem().getDetail().contains("the current status is not WORKING"));
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
                request.getCreatedAt().toString()
        ).block();

        NationalRegistriesRequest updated = getRequestByBatchId(batchId);
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
                RequestStatusEnum.NOT_WORKED
        );

        repository.putRequest(request).block();

        PnInternalException exception = Assertions.assertThrows(
                PnInternalException.class,
                () -> repository.updateStatusToError(
                        request.getCorrelationId(),
                        request.getCreatedAt().toString()
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to ERROR"));
        Assertions.assertTrue(exception.getProblem().getDetail().contains("the current status is not WORKING"));
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
                        request.getCreatedAt().toString()
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("Unable to transition to ERROR"));
        Assertions.assertTrue(exception.getProblem().getDetail().contains("the current status is not WORKING"));
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
                        null
                ).block()
        );

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
                        " "
                ).block()
        );

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
                        null
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("ResultPayload is required"));
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
                        " "
                ).block()
        );

        Assertions.assertTrue(exception.getProblem().getDetail().contains("ResultPayload is required"));
    }

    private NationalRegistriesRequest getRequestByBatchId(String batchId ) {
        Page<NationalRegistriesRequest> page = repository.getRequestsBatchId(batchId, null).block();
        Assertions.assertNotNull(page);
        Assertions.assertEquals(1, page.items().size());
        return page.items().getFirst();
    }

    private NationalRegistriesRequest buildRequest(String correlationId, Instant createdAt, String batchId, GatewayDownstreamService registry, RequestStatusEnum status) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId(correlationId);
        request.setCreatedAt(createdAt);
        request.setTaxId("RSSMRA80A01H501U");
        request.setRecipientType("PF");
        request.setClientId("pn-delivery");
        request.setRegistry(registry);
        request.setStatus(status);
        request.setRegistryStatus(registry.name() + "#" + status.name());
        request.setBatchId(batchId);
        request.setReferenceRequestDate("2026-10-07T10:00:00Z");
        request.setUpdatedAt(createdAt);
        request.setTtl(createdAt.plus(30,ChronoUnit.DAYS).getEpochSecond());
        return request;
    }
}