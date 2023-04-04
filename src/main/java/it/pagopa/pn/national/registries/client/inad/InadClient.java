package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
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

@Component
@Slf4j
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
        return accessTokenExpiringMap.getToken(purposeId, inadSecretConfig.getInadPdndSecretValue())
                .flatMap(tokenEntry -> callExtract(taxId, practicalReference, tokenEntry))
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InadResponseKO.class);
                    }
                })
                .retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                );
    }

    private Mono<ResponseRequestDigitalAddressDto> callExtract(String taxId, String practicalReference, AccessTokenCacheEntry tokenEntry) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("practicalReference", practicalReference)
                        .path("/extract/{codice_fiscale}")
                        .build(Map.of("codice_fiscale", taxId)))
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(tokenEntry.getAccessToken());
                })
                .retrieve()
                .bodyToMono(ResponseRequestDigitalAddressDto.class);
    }

    protected boolean checkExceptionType(Throwable throwable) {
        log.debug("Try Retry call to INAD");
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
