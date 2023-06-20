package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
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

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_INAD_ADDRESS;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INAD_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class InadClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;
    private final InadSecretConfig inadSecretConfig;

    protected InadClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         InadWebClient inadWebClient,
                         @Value("${pn.national.registries.pdnd.inad.purpose-id}") String purposeId,
                         InadSecretConfig inadSecretConfig) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.purposeId = purposeId;
        this.webClient = inadWebClient.init();
        this.inadSecretConfig = inadSecretConfig;
    }

    public Mono<ResponseRequestDigitalAddressDto> callEService(String taxId, String practicalReference) {
        return accessTokenExpiringMap.getPDNDToken(purposeId, inadSecretConfig.getInadPdndSecretValue(), false)
                .flatMap(tokenEntry -> callExtract(taxId, practicalReference, tokenEntry))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                // don't pass retrySignal.failure() to PnInternalException as cause
                                // the common handler prints the stack trace that includes the called URL and for INAD in the URL is the taxId
                                new PnInternalException(ERROR_MESSAGE_INAD_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED))
                );
    }

    private Mono<ResponseRequestDigitalAddressDto> callExtract(String taxId, String practicalReference, AccessTokenCacheEntry tokenEntry) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PROCESS_SERVICE_INAD_ADDRESS);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("practicalReference", practicalReference)
                        .path("/extract/{codice_fiscale}")
                        .build(Map.of("codice_fiscale", taxId)))
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(tokenEntry.getTokenValue());
                })
                .retrieve()
                .bodyToMono(ResponseRequestDigitalAddressDto.class)
                .doOnError(throwable -> {
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InadResponseKO.class);
                    }
                });
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            log.debug("Try Retry call to INAD");
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
