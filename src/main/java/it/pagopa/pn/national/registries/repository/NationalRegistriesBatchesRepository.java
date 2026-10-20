package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;


public interface NationalRegistriesBatchesRepository {

    Mono<NationalRegistriesBatch> putBatch (NationalRegistriesBatch batch);

    Mono<NationalRegistriesBatch> getBatch(String batchId);

    Mono<Page<NationalRegistriesBatch>> getBatchesToProcess(BatchesStatusEnum status, Instant now, Map<String, AttributeValue> lastEvaluatedKey);

    Mono<NationalRegistriesBatch> updateToFinalStatus(String pk, String sk, BatchesStatusEnum finalStatus);

    Mono<NationalRegistriesBatch> resetStatusForRetry(String pk, String sk, Integer retry, boolean isInProgressRetry, GatewayDownstreamService registry);

    Mono<NationalRegistriesBatch> updateItem(NationalRegistriesBatch batch, BatchesStatusEnum newStatus);
}