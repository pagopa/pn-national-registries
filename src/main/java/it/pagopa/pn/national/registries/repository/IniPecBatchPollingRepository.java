package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IniPecBatchPollingRepository {

    Mono<BatchPolling> createBatchPolling(BatchPolling batchPolling);
    Mono<BatchPolling> updateBatchPolling(BatchPolling batchPolling);
    Mono<List<BatchPolling>> getBatchPollingWithoutReservationIdAndStatusNotWork();
    Mono<List<BatchPolling>> setReservationIdToAndStatusToWorkBatchPolling(List<BatchPolling> batchPollings, String reservationId);
    Mono<List<BatchPolling>> getBatchPollingByReservationIdAndStatusToWork(String reservationId);

}
