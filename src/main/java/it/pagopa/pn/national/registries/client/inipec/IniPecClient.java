package it.pagopa.pn.national.registries.client.inipec;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECErrorDto;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.service.TokenProvider;
import it.pagopa.pn.national.registries.utils.InipecScopeEnum;
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
public class IniPecClient {

    private final WebClient webClient;
    private final IniPecJwsGenerator iniPecJwsGenerator;

    protected IniPecClient(IniPecWebClient iniPecWebClient,
                           IniPecJwsGenerator iniPecJwsGenerator) {
        webClient = iniPecWebClient.init();
        this.iniPecJwsGenerator = iniPecJwsGenerator;
    }

    public Mono<ClientCredentialsResponseDto> getToken(){
        String jws = iniPecJwsGenerator.createAuthRest();
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
        return getToken().flatMap(accessTokenCacheEntry ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/richiestaElencoPec")
                                .build(request))
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            httpHeaders.set("scope",InipecScopeEnum.PEC.value());
                        })
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

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
