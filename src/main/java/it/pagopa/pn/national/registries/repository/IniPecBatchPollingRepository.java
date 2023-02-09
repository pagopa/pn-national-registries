package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IniPecBatchPollingRepository {

    Mono<BatchPolling> create(BatchPolling batchPolling);

    Mono<BatchPolling> update(BatchPolling batchPolling);

    Mono<Page<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWorked(Map<String, AttributeValue> lastKey, int limit);

    Mono<BatchPolling> setNewReservationIdToBatchPolling(BatchPolling batchPolling);

    Mono<List<BatchPolling>> getBatchPollingToRecover();
}
