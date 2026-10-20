package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface NationalRegistriesRequestsRepository {

    Mono<NationalRegistriesRequest> putRequest(NationalRegistriesRequest nationalRegistriesRequest);

    Mono<Page<NationalRegistriesRequest>> getRequestsBatchId(String batchId, RequestStatusEnum status, Map<String, AttributeValue> lastEvaluatedKey);

    SdkPublisher<Page<NationalRegistriesRequest>> getRequestsByRegistryStatus(GatewayDownstreamService registry, int limit);

    Mono<NationalRegistriesRequest> updateStatusToWorking(String pk, String sk, String batchId, GatewayDownstreamService registry);

    Mono<NationalRegistriesRequest> updateStatusToWorked(String pk, String sk, String resultPayload, GatewayDownstreamService registry);

    Mono<NationalRegistriesRequest> resetStatusForRetry(String pk, String sk, Integer retry, GatewayDownstreamService registry);

    Mono<NationalRegistriesRequest> updateStatusToError(String pk, String sk, GatewayDownstreamService registry);

}
