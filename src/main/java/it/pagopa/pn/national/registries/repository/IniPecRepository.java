package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IniPecRepository {

    Mono<BatchRequest> saveRequestCF(GetDigitalAddressIniPECRequestBodyDto requestCF);
    Mono<List<BatchRequest>> processingRecords();
    Mono<Void> aggregateIdBatch(List<BatchRequest> batchRequest);

    Mono<List<BatchRequest>> getBatchRequestByBatchId(String batchId);
    Mono<BatchPolling> callIniPecAndAggregateCorrelationId(List<BatchRequest> batchRequests, String batchId);

}
