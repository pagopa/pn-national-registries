package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IniPecBatchPollingRepository {

    Mono<BatchPolling> createBatchPolling(BatchPolling batchPolling);
    Mono<BatchPolling> updateBatchPolling(BatchPolling batchPolling);
    Mono<Page<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWork(Map<String, AttributeValue> lastKey);
    Mono<List<BatchPolling>> setReservationIdToAndStatusWorkingBatchPolling(List<BatchPolling> batchPollings, String reservationId);
    Mono<List<BatchPolling>> getBatchPollingByReservationIdAndStatusWorking(String reservationId);
}
