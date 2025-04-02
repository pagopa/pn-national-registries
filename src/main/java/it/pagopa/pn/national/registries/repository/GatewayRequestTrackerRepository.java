package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import reactor.core.publisher.Mono;

public interface GatewayRequestTrackerRepository {
    Mono<GatewayRequestTrackerEntity> putIfAbsentOrRetrieve(
            GatewayRequestTrackerEntity entity
    );
}
