package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchResponse;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
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

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Slf4j
@Component
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

    public Mono<String> getToken(String scope) {
        String jws = infoCamereJwsGenerator.createAuthRest(scope);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/authentication")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build())
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .bodyValue(jws)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, retrySignal.failure()))
                );
    }

    public Mono<IniPecBatchResponse> callEServiceRequestId(IniPecBatchRequest request) {
        String requestJson = convertToJson(request);
        return getToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callRichiestaElencoPec(requestJson, token));
    }

    private Mono<IniPecBatchResponse> callRichiestaElencoPec(String body, String token) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/richiestaElencoPec")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set(SCOPE, InipecScopeEnum.PEC.value());
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(IniPecBatchResponse.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                }).retryWhen(Retry.max(1)
                        .filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, retrySignal.failure()))
                );
    }

    public Mono<IniPecPollingResponse> callEServiceRequestPec(String correlationId) {
        return getToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callGetElencoPec(correlationId, token));
    }

    private Mono<IniPecPollingResponse> callGetElencoPec(String correlationId, String token) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getElencoPec/{identificativoRichiesta}")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build(Map.of("identificativoRichiesta", correlationId))
                )
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set(SCOPE, InipecScopeEnum.PEC.value());
                })
                .retrieve()
                .bodyToMono(IniPecPollingResponse.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, retrySignal.failure()))
                );
    }

    public Mono<AddressRegistroImprese> getLegalAddress(String taxId) {
        return getToken(InipecScopeEnum.SEDE.value())
                .flatMap(token -> callGetLegalAddress(taxId, token));
    }

    private Mono<AddressRegistroImprese> callGetLegalAddress(String taxId, String token) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/sede/{cf}")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build(Map.of("cf", taxId)))
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set(SCOPE, InipecScopeEnum.SEDE.value());
                })
                .retrieve()
                .bodyToMono(AddressRegistroImprese.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_REGISTRO_IMPRESE, ERROR_CODE_REGISTRO_IMPRESE, retrySignal.failure()))
                );
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private String convertToJson(IniPecBatchRequest iniPecBatchRequest) {
        try {
            return mapper.writeValueAsString(iniPecBatchRequest);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }

    public Mono<InfoCamereVerification> checkTaxIdAndVatNumberInfoCamere(InfoCamereLegalRequestBodyFilterDto filterDto) {
        return getToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callCheckTaxId(filterDto, token));
    }

    private Mono<InfoCamereVerification> callCheckTaxId(InfoCamereLegalRequestBodyFilterDto filterDto, String token) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/legaleRappresentante/{cfPersona}")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .queryParam("cfImpresa", filterDto.getVatNumber())
                        .build(Map.of("cfPersona", filterDto.getTaxId())))
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(InfoCamereVerification.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_LEGALE_RAPPRESENTANTE, ERROR_CODE_LEGALE_RAPPRESENTANTE, retrySignal.failure()))
                );
    }
}
