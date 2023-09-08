package it.pagopa.pn.national.registries.log;

import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RequestResponseLoggingFilter implements WebFilter {

    private final String healthCheckPath;

    public RequestResponseLoggingFilter(@Value("${pn.national.registries.health-check-path}") String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest httpRequest = exchange.getRequest();

        if (healthCheckPath.equalsIgnoreCase(httpRequest.getURI().getPath())) {
            log.trace("request to health-check actuator");
            return chain.filter(exchange);
        }

        final String httpUrl = httpRequest.getURI().toString();
        Long startTime = System.currentTimeMillis();

        ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            String requestBody = "";

            @Override
            public @NotNull Flux<DataBuffer> getBody() {
                return super.getBody().publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                        requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                        log.info("Request HTTP {} to {} - body: {}", exchange.getRequest().getMethod(), httpUrl, MaskDataUtils.maskInformation(requestBody));
                    } catch (IOException e) {
                        log.info("Request HTTP {} to {} - body: {}", exchange.getRequest().getMethod(), httpUrl, MaskDataUtils.maskInformation(requestBody));
                    }
                });
            }
        };

        ServerHttpResponseDecorator loggingServerHttpResponseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            String responseBody = "";

            @Override
            public @NotNull Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
                Mono<DataBuffer> buffer = Mono.from(body);
                return super.writeWith(buffer.publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                        responseBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                        log.info("Response from {} - body: {} - timelapse: {}ms", httpUrl, MaskDataUtils.maskInformation(responseBody),
                                System.currentTimeMillis() - startTime);
                    } catch (Exception e) {
                        log.info("Response from {} - body: {} - timelapse: {}ms", httpUrl, MaskDataUtils.maskInformation(responseBody),
                                System.currentTimeMillis() - startTime);
                    }
                }));
            }
        };

        return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).response(loggingServerHttpResponseDecorator).build());
    }

}
