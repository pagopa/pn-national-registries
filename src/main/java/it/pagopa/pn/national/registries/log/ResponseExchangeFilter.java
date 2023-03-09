package it.pagopa.pn.national.registries.log;

import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
public class ResponseExchangeFilter implements ExchangeFilterFunction {

    @Override
    public @NotNull Mono<ClientResponse> filter(@NotNull ClientRequest request, ExchangeFunction next) {
        long start = System.currentTimeMillis();
        return next.exchange(interceptBody(request))
                .map(response -> interceptBody(start, response, request));
    }

    private ClientResponse interceptBody(long startTime, ClientResponse response, ClientRequest request) {
        StringBuilder body = new StringBuilder();
        return response.mutate()
                .body(data -> data
                        .doOnNext(dataBuffer -> body.append(dataBuffer.toString(StandardCharsets.UTF_8)))
                        .doOnComplete(() -> logResponseBody(startTime, body.toString(), response, request)))
                .build();
    }

    public void logResponseBody(long startTime, String body, ClientResponse response, ClientRequest request) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Response HTTP from {} {} {} - body: {} - timelapse: {}ms",
                MaskDataUtils.maskInformation(request.url().toString()),
                response.statusCode().value(),
                Objects.requireNonNull(response.statusCode().name()),
                MaskDataUtils.maskInformation(body),
                duration);
    }

    private ClientRequest interceptBody(ClientRequest request) {
        return ClientRequest.from(request)
                .body((outputMessage, context) -> request.body().insert(new ClientHttpRequestDecorator(outputMessage) {
                    @Override public @NotNull Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
                        return super.writeWith(Mono.from(body)
                                .doOnNext(dataBuffer -> logRequestBody(dataBuffer, request)));
                    }
                }, context))
                .build();
    }

    public void logRequestBody(DataBuffer dataBuffer, ClientRequest request) {
        log.info("Request HTTP {} to: {} - body: {}",
                request.method().name(),
                MaskDataUtils.maskInformation(request.url().toString()),
                MaskDataUtils.maskInformation(dataBuffer.toString(StandardCharsets.UTF_8)));
    }

}
