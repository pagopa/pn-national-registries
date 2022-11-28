package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchRequest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IniPecBatchRequestRepository {

    Mono<BatchRequest> createBatchRequest(BatchRequest batchRequest);

    Mono<Page<BatchRequest>> getBatchRequestByNotBatchIdPageable(Map<String, AttributeValue> lastKey);

    Mono<List<BatchRequest>> getBatchRequestsToSend(String batchId);

    Mono<BatchRequest> setBatchRequestsStatus(BatchRequest batchRequest, String status);

    Mono<List<BatchRequest>> setNewBatchIdToBatchRequests(List<BatchRequest> batchRequest, String batchId);

    Mono<List<BatchRequest>> resetBatchIdForRecovery();

    Mono<List<BatchRequest>> resetBatchIdToBatchRequests(List<BatchRequest> batchRequests);
}
