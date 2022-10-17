package it.pagopa.pn.national.registries.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.national.registries.model.WebClientResponseProblemDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Configuration
@Order(-2)
@Import(ExceptionHelper.class)
@Slf4j
public class PnWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ExceptionHelper exceptionHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PnWebExceptionHandler(ExceptionHelper exceptionHelper){
        this.exceptionHelper = exceptionHelper;
        objectMapper.findAndRegisterModules();
        objectMapper
                .configOverride(OffsetDateTime.class)
                .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange serverWebExchange, @NonNull Throwable throwable) {
        DataBuffer dataBuffer;
        DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
        Problem problem;
        try {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException exception = (WebClientResponseException) throwable;
                log.error("Error -> statusCode: {}, message: {}",exception.getStatusCode().value(),exception.getMessage());
                problem = createProblem(exception);
            } else {
                log.error("Error -> {}, uri : {}",throwable.getMessage(), serverWebExchange.getRequest().getURI());
                problem = exceptionHelper.handleException(throwable);
            }
            problem.setTraceId(MDC.get("trace_id"));
            problem.setTimestamp(OffsetDateTime.now());
            serverWebExchange.getResponse().setStatusCode(HttpStatus.resolve(problem.getStatus()));

            dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(problem));
        } catch (JsonProcessingException e) {
            log.error("cannot output problem", e);
            dataBuffer = bufferFactory.wrap(exceptionHelper.generateFallbackProblem().getBytes(StandardCharsets.UTF_8));
        }
        serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private Problem createProblem(WebClientResponseException exception) {
        String error = exception.getResponseBodyAsString();
        Problem problemDef = new Problem();
        WebClientResponseProblemDto problem;
        try {
            problem = objectMapper.readValue(error, WebClientResponseProblemDto.class);
            problemDef.setTitle(problem.getTitle());
            problemDef.setDetail(problem.getDetail());
            problemDef.setStatus(problem.getStatus());
        } catch (JsonProcessingException e) {
            log.error("error during parse Exception -> ",e);
            problemDef.setStatus(exception.getStatusCode().value());
            problemDef.setTitle(exception.getStatusText());
            problemDef.setDetail(exception.getMessage());
        }
        return problemDef;
    }
}
