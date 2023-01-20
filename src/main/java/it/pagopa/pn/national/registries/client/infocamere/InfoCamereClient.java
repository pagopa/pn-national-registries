package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Component
@Slf4j
public class InfoCamereClient {

    private final WebClient webClient;
    private final InfoCamereJwsGenerator infoCamereJwsGenerator;

    private final String clientId;

    private final ObjectMapper mapper;

    private static final String CLIENT_ID = "client_id";
    private static final String SCOPE = "scope";

    protected InfoCamereClient(InfoCamereWebClient infoCamereWebClient,
                               @Value("${pn.national.registries.infocamere.client-id}") String clientId,
                               InfoCamereJwsGenerator infoCamereJwsGenerator,
                               ObjectMapper mapper) {
        webClient = infoCamereWebClient.init();
        this.clientId = clientId;
        this.infoCamereJwsGenerator = infoCamereJwsGenerator;
        this.mapper = mapper;
    }

    public Mono<ClientCredentialsResponseDto> getToken(String scope){
        String jws = infoCamereJwsGenerator.createAuthRest(scope);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/authentication")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build())
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .body(Mono.just(jws), String.class)
                .retrieve()
                .bodyToMono(ClientCredentialsResponseDto.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
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
        return getToken(InipecScopeEnum.PEC.value()).flatMap(accessTokenCacheEntry ->
                webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/richiestaElencoPec")
                                .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set(SCOPE, InipecScopeEnum.PEC.value());
                        })
                        .bodyValue(requestJson)
                        .retrieve()
                        .bodyToMono(ResponsePollingIdIniPec.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
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
        return getToken(InipecScopeEnum.PEC.value()).flatMap(accessTokenCacheEntry ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/getElencoPec/{identificativoRichiesta}")
                                .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                                .build(Map.of("identificativoRichiesta", correlationId))
                )
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set(SCOPE,InipecScopeEnum.PEC.value());
                        })
                        .retrieve()
                        .bodyToMono(ResponsePecIniPec.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
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
        return getToken(InipecScopeEnum.SEDE.value()).flatMap(accessTokenCacheEntry ->
                webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/sede/{cf}")
                                .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                                .build(Map.of("cf", taxId)))
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set(SCOPE,InipecScopeEnum.SEDE.value());
                        })
                        .retrieve()
                        .bodyToMono(AddressRegistroImpreseResponse.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                                throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                        ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                        Charset.defaultCharset(), GetAddressRegistroImpreseErrorDto.class);
                            }
                        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new PnInternalException(ERROR_MESSAGE_REGISTRO_IMPRESE, ERROR_CODE_REGISTRO_IMPRESE, retrySignal.failure())))
        );
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private String convertToJson(RequestCfIniPec requestCfIniPec) {
        try {
            return mapper.writeValueAsString(requestCfIniPec);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INI_PEC, ERROR_CODE_INI_PEC,e);
        }
    }


    public Mono<InfoCamereVerificationResponse> checkTaxIdAndVatNumberInfoCamere(InfoCamereLegalRequestBodyFilterDto filterDto) {
        return getToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value()).flatMap(clientCredentialsResponseDto ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/legaleRappresentante/{cfPersona}")
                                .queryParam("cfImpresa", filterDto.getVatNumber())
                                .build(Map.of("cfPersona", filterDto.getTaxId())))
                        .headers(httpHeaders -> {
                            httpHeaders.setBearerAuth(clientCredentialsResponseDto.getAccessToken());
                        })
                        .retrieve()
                        .bodyToMono(InfoCamereVerificationResponse.class)
                        .doOnError(throwable -> {
                            if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                        ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                        Charset.defaultCharset(), InfoCamereLegalErrorDto.class);
                            }
                        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new PnInternalException(ERROR_MESSAGE_LEGALE_RAPPRESENTANTE, ERROR_CODE_LEGALE_RAPPRESENTANTE, retrySignal.failure())))
        );
    }
}
