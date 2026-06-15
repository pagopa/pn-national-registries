package it.pagopa.pn.national.registries.service;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface ReactiveRedisService<T> {
    String NAMESPACE="pn-emd::";

    Mono<T> get(String key);

    Mono<Void> set(String key, T value);

    Mono<Void> set(String key, T value, Duration ttl);

    Mono<Void> delete(String key);
}

