package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_ADDRESS_ANPR;
import static reactor.core.Exceptions.isRetryExhausted;

@Component
@Slf4j
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final CheckCfSecretConfig checkCfSecretConfig;

    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            CheckCfWebClient checkCfWebClient,
                            @Value("${pn.national.registries.pdnd.agenzia-entrate.purpose-id}") String purposeId,
                            ObjectMapper objectMapper,
                            CheckCfSecretConfig checkCfSecretConfig) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.purposeId = purposeId;
        webClient = checkCfWebClient.init();
        this.mapper = objectMapper;
        this.checkCfSecretConfig = checkCfSecretConfig;
    }

    public Mono<TaxIdVerification> callEService(Request richiesta) {
        return accessTokenExpiringMap.getToken(purposeId, checkCfSecretConfig.getCheckCfSecretValue())
                .flatMap(accessTokenCacheEntry -> {
                    String s = convertToJson(richiesta);
                    return webClient.post()
                            .uri("/verifica")
                            .headers(httpHeaders -> {
                                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                                httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                                httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                            })
                            .bodyValue(s)
                            .retrieve()
                            .bodyToMono(TaxIdVerification.class)
                            .doOnError(throwable -> {
                                if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                                    throw new PnNationalRegistriesException(ex.getMessage(),ex.getStatusCode().value(),
                                            ex.getStatusText(),ex.getHeaders(),ex.getResponseBodyAsByteArray(),
                                            Charset.defaultCharset(), TaxIdResponseKO.class);
                                }

                                if(isRetryExhausted(throwable) && throwable.getCause() instanceof WebClientResponseException.TooManyRequests tmrEx) {
                                    throw new PnNationalRegistriesException(tmrEx.getMessage(),tmrEx.getStatusCode().value(),
                                            tmrEx.getStatusText(),tmrEx.getHeaders(),tmrEx.getResponseBodyAsByteArray(),
                                            Charset.defaultCharset(), TaxIdResponseKO.class);
                                }
                            });
                }).retryWhen(Retry.max(1).filter(throwable -> {
                            log.debug("Try Retry call to CheckCf");
                            return checkExceptionType(throwable);
                        }).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                            retrySignal.failure()));
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private String convertToJson(Request richiesta) {
        try {
            return mapper.writeValueAsString(richiesta);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR, e);
        }
    }
}
