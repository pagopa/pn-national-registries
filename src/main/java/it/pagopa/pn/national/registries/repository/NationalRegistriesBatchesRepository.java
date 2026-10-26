package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;


public interface NationalRegistriesBatchesRepository {

    Mono<NationalRegistriesBatch> putBatch (NationalRegistriesBatch batch);

    Mono<NationalRegistriesBatch> getBatch(String batchId);

    SdkPublisher<Page<NationalRegistriesBatch>> getBatchesToProcess(BatchesStatusEnum status, Instant now);

    Mono<NationalRegistriesBatch> updateToFinalStatus(String pk, String sk, BatchesStatusEnum finalStatus);

    Mono<NationalRegistriesBatch> resetStatusForRetry(String pk, String sk, Integer retry, boolean isInProgressRetry, GatewayDownstreamService registry);

    Mono<NationalRegistriesBatch> updateItem(NationalRegistriesBatch batch, BatchesStatusEnum newStatus);
}