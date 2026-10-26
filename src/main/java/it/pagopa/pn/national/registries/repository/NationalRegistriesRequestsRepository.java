package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;

public interface NationalRegistriesRequestsRepository {

    Mono<NationalRegistriesRequest> putRequest(NationalRegistriesRequest nationalRegistriesRequest);

    SdkPublisher<Page<NationalRegistriesRequest>> getRequestsBatchId(String batchId, RequestStatusEnum status);

    SdkPublisher<Page<NationalRegistriesRequest>> getRequestsByRegistryStatus(GatewayDownstreamService registry, RequestStatusEnum status, int limit);

    Mono<NationalRegistriesRequest> updateStatusToWorking(String pk, String sk, String batchId, GatewayDownstreamService registry);

    Mono<NationalRegistriesRequest> updateStatusToWorked(String pk, String sk, String resultPayload, GatewayDownstreamService registry, boolean isFinalRegistry);

    Mono<NationalRegistriesRequest> resetStatusForRetry(String pk, String sk, Integer retry, Instant attemptAfter, GatewayDownstreamService registry);

    Mono<NationalRegistriesRequest> updateStatusToError(String pk, String sk, GatewayDownstreamService registry);

}
