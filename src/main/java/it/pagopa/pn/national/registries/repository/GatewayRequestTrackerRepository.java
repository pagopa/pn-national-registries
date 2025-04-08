package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import reactor.core.publisher.Mono;

public interface GatewayRequestTrackerRepository {
    /**
     * Create and put an item in the table based on given correlationId.
     * If correlationId already exists, retrieve the existing item.
     * @param correlationId correlationId of the item to put
     * @return the item that was put or the already existing item
     */
    Mono<GatewayRequestTrackerEntity> putIfAbsentOrRetrieve(
            String correlationId
    );
}
