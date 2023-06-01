package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.anpr.E002RequestDto;
import it.pagopa.pn.national.registries.model.anpr.ResponseE002OKDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_ANPR_ADDRESS;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
@lombok.CustomLog
public class AnprClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;
    private final AgidJwtSignature agidJwtSignature;
    private final ObjectMapper mapper;
    private final AnprSecretConfig anprSecretConfig;

    protected AnprClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         ObjectMapper mapper,
                         AgidJwtSignature agidJwtSignature,
                         AnprWebClient anprWebClient,
                         @Value("${pn.national.registries.pdnd.anpr.purpose-id}") String purposeId,
                         AnprSecretConfig anprSecretConfig) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.agidJwtSignature = agidJwtSignature;
        this.purposeId = purposeId;
        this.mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.anprSecretConfig = anprSecretConfig;
        webClient = anprWebClient.init();
    }

    public Mono<ResponseE002OKDto> callEService(E002RequestDto requestDto) {
        return accessTokenExpiringMap.getPDNDToken(purposeId, anprSecretConfig.getAnprPdndSecretValue())
                .flatMap(tokenEntry -> callAnpr(requestDto, tokenEntry))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ANPR_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<ResponseE002OKDto> callAnpr(E002RequestDto requestDto, AccessTokenCacheEntry tokenEntry) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PROCESS_SERVICE_ANPR_ADDRESS);
        String s = convertToJson(requestDto);
        String digest = createDigestFromPayload(s);
        log.debug("digest: {}", digest);
        return webClient.post()
                .uri("/anpr-service-e002")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(tokenEntry.getTokenValue());
                    httpHeaders.add("Agid-JWT-Signature", agidJwtSignature.createAgidJwt(digest));
                    httpHeaders.add("Content-Encoding", "UTF-8");
                    httpHeaders.add("Digest", digest);
                    httpHeaders.add("bearerAuth", tokenEntry.getTokenValue());
                })
                .bodyValue(s)
                .retrieve()
                .bodyToMono(ResponseE002OKDto.class)
                .doOnError(throwable -> {
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), AnprResponseKO.class);
                    }
                });
    }

    private String convertToJson(E002RequestDto requestDto) {
        try {
            return mapper.writeValueAsString(requestDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR, e);
        }
    }

    private String createDigestFromPayload(String request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
            return "SHA-256=" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR, e);
        }
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            log.debug("Try Retry call to ANPR");
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
