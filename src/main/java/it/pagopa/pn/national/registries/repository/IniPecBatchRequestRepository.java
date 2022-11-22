package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IniPecBatchRequestRepository {

    Mono<BatchRequest> createBatchRequest(BatchRequest batchRequest);
    Mono<BatchRequest> createBatchRequestByCf(GetDigitalAddressIniPECRequestBodyDto requestCF);
    Mono<Page<BatchRequest>> getBatchRequestByNotBatchIdPageable(Map<String, AttributeValue> lastKey);
    Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId);
    Mono<List<BatchRequest>> getBatchRequestToRecovery();
    Mono<List<BatchRequest>> setNewBatchIdToBatchRequests(List<BatchRequest> batchRequest, String batchId);
    Mono<List<BatchRequest>> setStatusToBatchRequests(List<BatchRequest> batchRequests, String status);
    Mono<List<BatchRequest>> resetBatchIdToBatchRequests(List<BatchRequest> batchRequests);

}
