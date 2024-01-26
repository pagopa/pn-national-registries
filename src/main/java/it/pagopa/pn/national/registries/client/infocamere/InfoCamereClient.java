package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
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

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
@lombok.CustomLog
public class InfoCamereClient {

    private final WebClient webClient;
    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String clientId;
    private final ObjectMapper mapper;

    private static final String CLIENT_ID = "client_id";
    private static final String SCOPE = "scope";

    protected InfoCamereClient(InfoCamereWebClient infoCamereWebClient,
                               @Value("${pn.national.registries.infocamere.client-id}") String clientId,
                               AccessTokenExpiringMap accessTokenExpiringMap,
                               ObjectMapper mapper) {
        this.clientId = clientId;
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.mapper = mapper;
        webClient = infoCamereWebClient.init();
    }

    public Mono<IniPecBatchResponse> callEServiceRequestId(IniPecBatchRequest request) {
        String requestJson = convertToJson(request);
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callRichiestaElencoPec(requestJson, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<IniPecBatchResponse> callRichiestaElencoPec(String body, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Retrieving correlationId [INFOCAMERE]");
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
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }

    public Mono<IniPecPollingResponse> callEServiceRequestPec(String correlationId) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callGetElencoPec(correlationId, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<IniPecPollingResponse> callGetElencoPec(String correlationId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Getting elencoPec InfoCamere for correlationId");
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
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }

    public Mono<AddressRegistroImprese> getLegalAddress(String taxId) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.SEDE.value())
                .flatMap(token -> callGetLegalAddress(taxId, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<AddressRegistroImprese> callGetLegalAddress(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS);
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
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }

    public Mono<InfoCamereLegalInstituionsResponse> getLegalInstitutions(CheckTaxIdRequestBodyFilterDto filter) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callGetLegalInstitutions(filter.getTaxId(), token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    public Mono<InfoCamereLegalInstituionsResponse> callGetLegalInstitutions(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/listaLegaleRappresentante/{cfPersona}")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build(Map.of("cfPersona", taxId)))
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set(SCOPE, InipecScopeEnum.LEGALE_RAPPRESENTANTE.value());
                })
                .retrieve()
                .bodyToMono(InfoCamereLegalInstituionsResponse.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }

    public Mono<InfoCamereVerification> checkTaxIdAndVatNumberInfoCamere(InfoCamereLegalRequestBodyFilterDto filterDto) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callCheckTaxId(filterDto, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<InfoCamereVerification> callCheckTaxId(InfoCamereLegalRequestBodyFilterDto filterDto, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL);
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
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }

    protected boolean shouldRetry(Throwable throwable) {
        return isUnauthorized(throwable);
    }

    private boolean isUnauthorized(Throwable throwable) {
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
}
