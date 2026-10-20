package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InadPollingService {

    public Mono<Void> handleMessage(PnPollingEvent.Payload payload) {
        return null;
    }
}
