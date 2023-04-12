package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdResponseKO;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
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
import java.util.List;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;
import static reactor.core.Exceptions.isRetryExhausted;

@Slf4j
@Component
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final CheckCfSecretConfig checkCfSecretConfig;

    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            CheckCfWebClient checkCfWebClient,
                            @Value("${pn.national.registries.pdnd.ade-check-cf.purpose-id}") String purposeId,
                            ObjectMapper objectMapper,
                            CheckCfSecretConfig checkCfSecretConfig) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.purposeId = purposeId;
        this.mapper = objectMapper;
        this.checkCfSecretConfig = checkCfSecretConfig;
        webClient = checkCfWebClient.init();
    }

    public Mono<TaxIdVerification> callEService(Request richiesta) {
        return accessTokenExpiringMap.getToken(purposeId, checkCfSecretConfig.getCheckCfPdndSecretValue())
                .flatMap(tokenEntry -> callVerifica(richiesta, tokenEntry))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ADE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<TaxIdVerification> callVerifica(Request richiesta, AccessTokenCacheEntry tokenEntry) {
        String s = convertToJson(richiesta);
        return webClient.post()
                .uri("/verifica")
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    httpHeaders.setBearerAuth(tokenEntry.getAccessToken());
                })
                .bodyValue(s)
                .retrieve()
                .bodyToMono(TaxIdVerification.class)
                .doOnError(throwable -> {
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), TaxIdResponseKO.class);
                    }
                    if (isRetryExhausted(throwable) && throwable.getCause() instanceof WebClientResponseException.TooManyRequests e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), TaxIdResponseKO.class);
                    }
                });
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            log.debug("Try Retry call to CheckCf");
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private String convertToJson(Request richiesta) {
        try {
            return mapper.writeValueAsString(richiesta);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}
