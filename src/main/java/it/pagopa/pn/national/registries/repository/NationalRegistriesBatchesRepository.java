package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;


public interface NationalRegistriesBatchesRepository {

    Mono<NationalRegistriesBatch> putBatch (NationalRegistriesBatch batch);

    Mono<NationalRegistriesBatch> getBatch(String batchId);

    Mono<Page<NationalRegistriesBatch>> getBatchesToProcess(String status, Instant now, Map<String, AttributeValue> lastEvaluatedKey);

    Mono<NationalRegistriesBatch> updateStatusAndAttemptAfter(NationalRegistriesBatch batch, String fromStatus, String toStatus, String errorType, Boolean isError);
}