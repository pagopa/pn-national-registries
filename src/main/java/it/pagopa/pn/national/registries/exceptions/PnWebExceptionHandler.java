package it.pagopa.pn.national.registries.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.national.registries.model.NationalRegistriesProblem;
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
        NationalRegistriesProblem nationalRegistriesProblem;
        try {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException exception = (WebClientResponseException) throwable;
                log.error("Error -> statusCode: {}, message: {}, uri: {}", exception.getStatusCode().value(), exception.getMessage(), serverWebExchange.getRequest().getURI());
                nationalRegistriesProblem = createProblem(exception);
            }else{
                log.error("Error -> {}, uri : {}",throwable.getMessage(), serverWebExchange.getRequest().getURI());
                nationalRegistriesProblem = convertToNationalRegistriesProblem(exceptionHelper.handleException(throwable));
            }
            nationalRegistriesProblem.setTraceId(MDC.get("trace_id"));
            nationalRegistriesProblem.setTimestamp(OffsetDateTime.now());
            dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(nationalRegistriesProblem));
            serverWebExchange.getResponse().setStatusCode(HttpStatus.resolve(nationalRegistriesProblem.getStatus()));

        } catch (JsonProcessingException e) {
            log.error("cannot output problem", e);
            dataBuffer = bufferFactory.wrap(exceptionHelper.generateFallbackProblem().getBytes(StandardCharsets.UTF_8));
        }
        serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private NationalRegistriesProblem convertToNationalRegistriesProblem(Problem handleException) {
        NationalRegistriesProblem nationalRegistriesProblem = new NationalRegistriesProblem();
        nationalRegistriesProblem.setDetail(handleException.getDetail());
        nationalRegistriesProblem.setTitle(handleException.getTitle());
        nationalRegistriesProblem.setStatus(handleException.getStatus());
        nationalRegistriesProblem.setErrors(handleException.getErrors());
        return nationalRegistriesProblem;
    }

    private NationalRegistriesProblem createProblem(WebClientResponseException exception) throws JsonProcessingException {
        String error = exception.getResponseBodyAsString();
        NationalRegistriesProblem problemDef = new NationalRegistriesProblem();
        problemDef.setStatus(exception.getStatusCode().value());
        problemDef.setTitle(exception.getStatusText());
        problemDef.setDetail(exception.getMessage());
        problemDef.setErrors(objectMapper.readValue(error,Object.class));
        return problemDef;
    }
}
