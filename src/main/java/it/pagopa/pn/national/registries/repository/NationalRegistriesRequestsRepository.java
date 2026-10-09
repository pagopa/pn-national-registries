package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface NationalRegistriesRequestsRepository {

    Mono<NationalRegistriesRequest> putRequest(NationalRegistriesRequest nationalRegistriesRequest);

    Mono<Page<NationalRegistriesRequest>> getRequestsBatchId(String batchId, Map<String, AttributeValue> lastEvaluatedKey);

    Mono<Page<NationalRegistriesRequest>> getRequestsByRegistryStatus(GatewayDownstreamService registry, Map<String, AttributeValue> lastEvaluatedKey, int limit);

    Mono<NationalRegistriesRequest> updateStatusToWorking(String pk, String sk, String batchId);

    Mono<NationalRegistriesRequest> updateStatusToWorked(String pk, String sk, String resultPayload);

    Mono<NationalRegistriesRequest> updateStatusToError(String pk, String sk);
}
