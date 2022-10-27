package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.entity.CounterModel;
import reactor.core.publisher.Mono;

public interface CounterRepository {

    Mono<CounterModel> getCounter(String id);

}
