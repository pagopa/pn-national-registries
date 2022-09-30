package it.pagopa.pn.national.registries.log;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
public class ResponseExchangeFilter implements ExchangeFilterFunction {

    private static final String METRICS_WEBCLIENT_START_TIME = ResponseExchangeFilter.class.getName() + ".START_TIME";

    @Override
    public Mono<ClientResponse> filter(@NotNull ClientRequest request, ExchangeFunction next) {
        return next.exchange(request).doOnEach(signal -> {
            if (!signal.isOnComplete()) {
                Long startTime = signal.getContextView().get(METRICS_WEBCLIENT_START_TIME);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Response HTTP from {} {} {} - timelapse: {}ms",
                        request.url(),
                        Objects.requireNonNull(signal.get()).statusCode().value(),
                        Objects.requireNonNull(signal.get()).statusCode().name(),
                        duration);
            }
        }).contextWrite(ctx -> ctx.put(METRICS_WEBCLIENT_START_TIME, System.currentTimeMillis()));
    }
}
