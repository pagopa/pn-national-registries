package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
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

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

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
        return accessTokenExpiringMap.getToken(purposeId, inadSecretConfig.getInadSecretValue()).flatMap(accessTokenCacheEntry ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("practicalReference", practicalReference)
                                .path("/extract/{codice_fiscale}")
                                .build(taxId))
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                        })
                        .retrieve()
                        .bodyToMono(ResponseRequestDigitalAddressDto.class))
                .retryWhen(Retry.max(1).filter(this::checkExceptionType)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, retrySignal.failure())));
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
