package it.pagopa.pn.national.registries.exceptions;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.national.registries.model.NationalRegistriesProblem;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.anpr.ResponseKO;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
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
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;

@Slf4j
@Order(-2)
@Configuration
@Import(ExceptionHelper.class)
public class PnWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ExceptionHelper exceptionHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOG_EX = "Error -> statusCode: {}, message: {}, uri: {}";

    public PnWebExceptionHandler(ExceptionHelper exceptionHelper) {
        this.exceptionHelper = exceptionHelper;
        objectMapper.findAndRegisterModules();

        objectMapper
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configOverride(OffsetDateTime.class)
                .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange serverWebExchange, @NonNull Throwable throwable) {
        DataBuffer dataBuffer;
        DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
        try {
            NationalRegistriesProblem problem;
            if (throwable instanceof PnNationalRegistriesException exception) {
                if (exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                    problem = convertToNationalRegistriesProblem(exceptionHelper.handleException(throwable));
                } else {
                    problem = createProblem(exception);
                }
            } else if (Exceptions.isRetryExhausted(throwable) && throwable.getCause() instanceof WebClientResponseException.ServiceUnavailable exception) {
                problem = createProblem(exception);
            } else {
                problem = convertToNationalRegistriesProblem(exceptionHelper.handleException(throwable));
            }

            if (problem.getStatus() >= 500) {
                log.error(LOG_EX, problem.getStatus(), MaskDataUtils.maskInformation(throwable.getMessage()), serverWebExchange.getRequest().getURI());
            } else {
                log.warn(LOG_EX, problem.getStatus(), MaskDataUtils.maskInformation(throwable.getMessage()), serverWebExchange.getRequest().getURI());
            }

            problem.setTraceId(MDC.get(MDCWebFilter.MDC_TRACE_ID_KEY));
            problem.setTimestamp(OffsetDateTime.now());

            dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(problem));
            serverWebExchange.getResponse().setStatusCode(HttpStatus.resolve(problem.getStatus()));

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

    private NationalRegistriesProblem createProblem(PnNationalRegistriesException exception) throws JsonProcessingException {
        String error = exception.getResponseBodyAsString();
        NationalRegistriesProblem problemDef = new NationalRegistriesProblem();
        problemDef.setStatus(exception.getStatusCode().value());
        problemDef.setTitle(exception.getStatusText());
        problemDef.setDetail(exception.getMessage());
        if (exception.getClassName().equals(AnprResponseKO.class)) {
            problemDef.setErrors(mapToAnprResponseKO(error));
        } else if (!StringUtils.isNullOrEmpty(error)) {
            problemDef.setErrors(objectMapper.readValue(error, exception.getClassName()));
        } else {
            problemDef.setErrors(new ArrayList<>());
        }
        return problemDef;
    }

    private NationalRegistriesProblem createProblem(WebClientResponseException.ServiceUnavailable exception) {
        NationalRegistriesProblem problemDef = new NationalRegistriesProblem();
        problemDef.setStatus(exception.getStatusCode().value());
        problemDef.setTitle(exception.getStatusText());
        problemDef.setDetail(exception.getMessage());
        problemDef.setErrors(new ArrayList<>());
        return problemDef;
    }

    private AnprResponseKO mapToAnprResponseKO(String responseBodyAsString) throws JsonProcessingException {
        ResponseKO responseKO = new ResponseKO();
        if (!StringUtils.isNullOrEmpty(responseBodyAsString)) {
            responseKO = objectMapper.readValue(responseBodyAsString, ResponseKO.class);
        }
        AnprResponseKO anprResponseKO = new AnprResponseKO();
        if (responseKO.getResponseHeader() != null) {
            anprResponseKO.setClientOperationId(responseKO.getResponseHeader().getClientOperationId());
        }
        if (responseKO.getErrorsList() != null && responseKO.getErrorsList().size() == 1) {
            anprResponseKO.setCode(responseKO.getErrorsList().get(0).getCode());
            anprResponseKO.setDetail(responseKO.getErrorsList().get(0).getDetail());
            anprResponseKO.setElement(responseKO.getErrorsList().get(0).getElement());
        }
        return anprResponseKO;
    }
}
