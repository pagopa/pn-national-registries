package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECErrorDto;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Component
@Slf4j
public class InfoCamereClient {

    private final WebClient webClient;
    private final InfoCamereJwsGenerator infoCamereJwsGenerator;

    private final ObjectMapper mapper;

    protected InfoCamereClient(InfoCamereWebClient infoCamereWebClient,
                               InfoCamereJwsGenerator infoCamereJwsGenerator,
                               ObjectMapper mapper) {
        webClient = infoCamereWebClient.init();
        this.infoCamereJwsGenerator = infoCamereJwsGenerator;
        this.mapper = mapper;
    }

    public Mono<ClientCredentialsResponseDto> getToken(){
        String jws = infoCamereJwsGenerator.createAuthRest();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/token")
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(jws);
                })
                .retrieve()
                .bodyToMono(ClientCredentialsResponseDto.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) throwable;
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), GetDigitalAddressIniPECErrorDto.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, retrySignal.failure()))
                );
    }

    public Mono<ResponsePollingIdIniPec> callEServiceRequestId(RequestCfIniPec request) {
        String requestJson = convertToJson(request);
        return getToken().flatMap(accessTokenCacheEntry ->
                webClient.post()
                        .uri("/richiestaElencoPec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set("scope", InipecScopeEnum.PEC.value());
                        })
                        .bodyValue(requestJson)
                        .retrieve()
                        .bodyToMono(ResponsePollingIdIniPec.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                        ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                        Charset.defaultCharset(), GetDigitalAddressIniPECErrorDto.class);
                            }
                        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, retrySignal.failure()))
                        )
        );
    }

    public Mono<ResponsePecIniPec> callEServiceRequestPec(String correlationId) {
        return getToken().flatMap(accessTokenCacheEntry ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/getElencoPec/{identificativoRichiesta}")
                                .build(correlationId))
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set("scope",InipecScopeEnum.PEC.value());
                        })
                        .retrieve()
                        .bodyToMono(ResponsePecIniPec.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                        ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                        Charset.defaultCharset(), GetDigitalAddressIniPECErrorDto.class);
                            }
                        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, retrySignal.failure()))
                        )
        );
    }

    public Mono<AddressRegistroImpreseResponse> getLegalAddress(String taxId) {
        return getToken().flatMap(accessTokenCacheEntry ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/sede/{cf}")
                                .build(taxId))
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                        })
                        .retrieve()
                        .bodyToMono(AddressRegistroImpreseResponse.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                        ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                        Charset.defaultCharset(), GetAddressRegistroImpreseErrorDto.class);
                            }
                        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC, retrySignal.failure())))
        );
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private String convertToJson(RequestCfIniPec requestCfIniPec) {
        try {
            return mapper.writeValueAsString(requestCfIniPec);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR,e);
        }
    }
}
