package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.BaseTest;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.national.registries.constant.BatchesStatusEnum.*;

@Slf4j
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "pn.national-registries.dao.national-registries-batches-table-name=pn-nationalRegistries-nationalRegistriesBatches",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.polling-retry-after=60",
        "pn.national-registries.inad.polling-in-progress-retry-after=120",
        "pn.national-registries.inipec.ttl=3600"
})
class NationalRegistriesBatchesRepositoryIT extends BaseTest.WithLocalStack {

    @Autowired
    NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

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

    @Test
    void putAndGetBatchTest() {
        NationalRegistriesBatch batch = batch("put-get", REQUEST_SENT, Instant.parse("2026-10-09T10:00:00Z"));
        batch.setRegistry("INAD");
        batch.setBatchSize(10);

        NationalRegistriesBatch storedBatch = nationalRegistriesBatchesRepository.putBatch(batch).block();
        NationalRegistriesBatch retrievedBatch = nationalRegistriesBatchesRepository.getBatch(batch.getBatchId()).block();

        Assertions.assertEquals(batch, storedBatch);
        Assertions.assertNotNull(retrievedBatch);
        Assertions.assertEquals(batch.getBatchId(), retrievedBatch.getBatchId());
        Assertions.assertEquals("-", retrievedBatch.getSk());
        Assertions.assertEquals("INAD", retrievedBatch.getRegistry());
        Assertions.assertEquals(10, retrievedBatch.getBatchSize());
    }

    @Test
    void getBatchesToProcessFiltersByStatusAndAttemptAfterAndPaginatesTest() {
        Instant now = Instant.parse("2026-10-09T10:00:00Z");
        NationalRegistriesBatch firstBatch = batch("query-1", REQUEST_SENT, now.minus(2, ChronoUnit.MINUTES));
        NationalRegistriesBatch secondBatch = batch("query-2", REQUEST_SENT, now.minus(1, ChronoUnit.MINUTES));
        NationalRegistriesBatch thirdBatch = batch("query-3", REQUEST_SENT, now);
        NationalRegistriesBatch futureBatch = batch("query-future", REQUEST_SENT, now.plus(1, ChronoUnit.MINUTES));
        NationalRegistriesBatch otherStatusBatch = batch("query-other-status", WORKING, now.minus(1, ChronoUnit.MINUTES));

        List.of(firstBatch, secondBatch, thirdBatch, futureBatch, otherStatusBatch)
                .forEach(batch -> nationalRegistriesBatchesRepository.putBatch(batch).block());

        Page<NationalRegistriesBatch> firstPage = nationalRegistriesBatchesRepository
                .getBatchesToProcess(REQUEST_SENT, now, null)
                .block();

        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(2, firstPage.items().size());
        Assertions.assertTrue(firstPage.items().stream()
                .allMatch(batch -> REQUEST_SENT.equals(batch.getStatus()) && !batch.getAttemptAfter().isAfter(now)));
        Assertions.assertNotNull(firstPage.lastEvaluatedKey());

        Page<NationalRegistriesBatch> secondPage = nationalRegistriesBatchesRepository
                .getBatchesToProcess(REQUEST_SENT, now, firstPage.lastEvaluatedKey())
                .block();

        Assertions.assertNotNull(secondPage);
        Assertions.assertEquals(1, secondPage.items().size());
        Assertions.assertEquals(thirdBatch.getBatchId(), secondPage.items().get(0).getBatchId());
        Assertions.assertNull(secondPage.lastEvaluatedKey());
    }

    @Test
    void updateItemTest() {
        NationalRegistriesBatch batch = batch("update-technical-error", WORKING, Instant.parse("2026-10-09T10:00:00Z"));
        nationalRegistriesBatchesRepository.putBatch(batch).block();
        batch.setStatus(WORKED);

        NationalRegistriesBatch updatedBatch = nationalRegistriesBatchesRepository
                .updateItem(batch, WORKING)
                .block();
        NationalRegistriesBatch retrievedBatch = nationalRegistriesBatchesRepository.getBatch(batch.getBatchId()).block();

        Assertions.assertNotNull(updatedBatch);
        Assertions.assertNotNull(retrievedBatch);
        Assertions.assertEquals(WORKED, retrievedBatch.getStatus());
        Assertions.assertNotNull(retrievedBatch.getUpdatedAt());
        Assertions.assertTrue(retrievedBatch.getAttemptAfter().equals(Instant.parse("2026-10-09T10:00:00Z")));
    }

    @Test
    void updateItemWithInvalidFromStatus() {
        Instant attemptAfter = Instant.parse("2026-10-10T10:00:00Z");
        NationalRegistriesBatch batch = batch("update-without-error", ERROR, attemptAfter);
        nationalRegistriesBatchesRepository.putBatch(batch).block();

        batch.setStatus(WORKED);

        Assertions.assertThrows(ConditionalCheckFailedException.class, () -> nationalRegistriesBatchesRepository
                .updateItem(batch, WORKING)
                .block());
    }

    private NationalRegistriesBatch batch(String batchId, BatchesStatusEnum status, Instant attemptAfter) {
        NationalRegistriesBatch batch = new NationalRegistriesBatch();
        batch.setBatchId(batchId);
        batch.setStatus(status);
        batch.setAttemptAfter(attemptAfter);
        batch.setCreatedAt(Instant.parse("2026-10-09T09:00:00Z"));
        return batch;
    }

}
