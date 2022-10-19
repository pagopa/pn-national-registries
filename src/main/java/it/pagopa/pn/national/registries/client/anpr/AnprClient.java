package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.anpr.RichiestaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.RispostaE002OKDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Component
@Slf4j
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


    public Mono<RispostaE002OKDto> callEService(RichiestaE002Dto richiestaE002Dto){
        return accessTokenExpiringMap.getToken(purposeId,anprSecretConfig.getAnprSecretValue())
                .flatMap(accessTokenCacheEntry -> {
                    String s = convertToJson(richiestaE002Dto);
                    String digest = createDigestFromPayload(s);
                    log.debug("digest: {}",digest);
                    return webClient.post()
                            .uri("/anpr-service-e002")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(httpHeaders -> {
                                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                                httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                                httpHeaders.add("Agid-JWT-Signature", agidJwtSignature.createAgidJwt(digest));
                                httpHeaders.add("Content-Encoding", "UTF-8");
                                httpHeaders.add("Digest", digest);
                                httpHeaders.add("bearerAuth", accessTokenCacheEntry.getAccessToken());
                            })
                            .bodyValue(s)
                            .retrieve()
                            .bodyToMono(RispostaE002OKDto.class);
                }).retryWhen(Retry.max(1).filter(throwable -> {
                    log.debug("Try Retry call to ANPR");
                    return checkExceptionType(throwable);
                }).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                            new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, retrySignal.failure())));
    }

    private String convertToJson(RichiestaE002Dto richiestaE002Dto) {
        try {
            return mapper.writeValueAsString(richiestaE002Dto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR,e);
        }
    }

    private String createDigestFromPayload(String request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
            return "SHA-256="+Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR,e);
        }
    }

    protected boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }


}
