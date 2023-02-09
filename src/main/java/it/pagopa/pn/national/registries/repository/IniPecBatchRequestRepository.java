package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchRequest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IniPecBatchRequestRepository {

    Mono<BatchRequest> update(BatchRequest batchRequest);

    Mono<BatchRequest> create(BatchRequest batchRequest);

    Mono<Page<BatchRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit);

    Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId);

    Mono<BatchRequest> setNewBatchIdToBatchRequest(BatchRequest batchRequest);

    Mono<List<BatchRequest>> getBatchRequestToRecovery();

}
