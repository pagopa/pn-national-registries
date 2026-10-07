package it.pagopa.pn.national.registries.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.model.NationalRegistriesProblem;
import it.pagopa.pn.national.registries.model.agenziaentrate.AdEResponseKO;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.anpr.ErrorListAnpr;
import it.pagopa.pn.national.registries.model.anpr.ResponseKO;
import it.pagopa.pn.national.registries.model.ipa.IpaResponseKO;
import it.pagopa.pn.national.registries.model.pdnd.PdndResponseError;
import it.pagopa.pn.national.registries.model.pdnd.PdndResponseKO;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ADE;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_IPA;

@Slf4j
@Order(-2)
@Configuration
@Import(ExceptionHelper.class)
public class PnWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ExceptionHelper exceptionHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOG_EX = "Error -> statusCode: {}, message: {}, uri: {}";
    private static final int STATUS_OVER_500 = 500;

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

            if (problem.getStatus() >= STATUS_OVER_500) {
                log.error(LOG_EX, problem.getStatus(), throwable.getMessage(), serverWebExchange.getRequest().getURI());
            } else {
                log.warn(LOG_EX, problem.getStatus(), throwable.getMessage(), serverWebExchange.getRequest().getURI());
            }

            problem.setTraceId(MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
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
        String responseBody = exception.getResponseBodyAsString();
        NationalRegistriesProblem problemDef = new NationalRegistriesProblem();
        problemDef.setStatus(exception.getStatusCode().value());
        problemDef.setTitle(exception.getStatusText());
        problemDef.setDetail(exception.getMessage());
        if (exception.getClassName().equals(AnprResponseKO.class)) {
            problemDef.setErrors(mapToAnprResponseKO(responseBody));
        } else if (exception.getClassName().equals(IPAPecErrorDto.class)) {
            problemDef.setErrors(List.of(mapToIpaResponseKO(exception.getResponseBodyAsString())));
        } else if (exception.getClassName().equals(PdndResponseKO.class)) {
            problemDef.setErrors(mapToPdndResponseKO(responseBody));
        } else if (exception.getClassName().equals(ADELegalErrorDto.class)){
            problemDef.setErrors(mapToAdEResponseKo(responseBody));
        } else if (StringUtils.hasText(responseBody)) {
            problemDef.setErrors(List.of(objectMapper.readValue(responseBody, exception.getClassName())));
        } else {
            problemDef.setErrors(Collections.emptyList());
        }
        return problemDef;
    }

    private NationalRegistriesProblem createProblem(WebClientResponseException.ServiceUnavailable exception) {
        NationalRegistriesProblem problemDef = new NationalRegistriesProblem();
        problemDef.setStatus(exception.getStatusCode().value());
        problemDef.setTitle(exception.getStatusText());
        problemDef.setDetail(exception.getMessage());
        problemDef.setErrors(Collections.emptyList());
        return problemDef;
    }

    private List<AnprResponseKO> mapToAnprResponseKO(String responseBody) throws JsonProcessingException {
        ResponseKO responseKO = new ResponseKO();
        if (StringUtils.hasText(responseBody)) {
            responseKO = objectMapper.readValue(responseBody, ResponseKO.class);
        }
        String clientOperationId = responseKO.getAnprOperationId();
        List<AnprResponseKO> responseErrors = Collections.emptyList();
        if (responseKO.getErrorsList() != null) {
            responseErrors = responseKO.getErrorsList().stream()
                    .map(e -> mapToAnprResponseKO(clientOperationId, e))
                    .toList();
        }
        return responseErrors;
    }

    private AnprResponseKO mapToAnprResponseKO(String clientOperationId, ErrorListAnpr error) {
        AnprResponseKO response = new AnprResponseKO();
        response.setCode(error.getCode());
        response.setDetail(error.getDetail());
        response.setElement(error.getElement());
        response.setClientOperationId(clientOperationId);
        return response;
    }

    private List<PdndResponseError> mapToPdndResponseKO(String responseBody) throws JsonProcessingException {
        PdndResponseKO response = objectMapper.readValue(responseBody, PdndResponseKO.class);
        if (response.getErrors() != null) {
            return response.getErrors();
        }
        return Collections.emptyList();
    }

    private AdEResponseKO mapToAdEResponseKo(String responseBody) {
        AdEResponseKO adEResponseKO = new AdEResponseKO();
        if (StringUtils.hasText(responseBody)) {
            extractTagContent(responseBody, "faultstring",adEResponseKO);
        }
        adEResponseKO.setCode(ERROR_CODE_ADE);
        return adEResponseKO;
    }

    private void extractTagContent(String xml, String tagName,AdEResponseKO adEResponseKO) {
        if (StringUtils.hasText(xml) && StringUtils.hasText(tagName)) {
            String regex = String.format("<%s[^>]*>(.*?)</%s>", tagName, tagName);
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                adEResponseKO.setDetail(matcher.group(1).trim());
            }
        }
    }

    private IpaResponseKO mapToIpaResponseKO(String errorDetail) {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setDetail(errorDetail);
        ipaResponseKO.setCode(ERROR_CODE_IPA);
        return ipaResponseKO;
    }
}
