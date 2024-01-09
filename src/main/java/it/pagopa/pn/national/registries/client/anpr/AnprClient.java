package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.anpr.E002RequestDto;
import it.pagopa.pn.national.registries.model.anpr.ResponseE002OKDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.xml.bind.DatatypeConverter;
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
    private final AnprWebClient anprWebClient;
    private final AgidJwtSignature agidJwtSignature;
    private final AgidJwtTrackingEvidence agidJwtTrackingEvidence;
    private final AnprSecretConfig anprSecretConfig;

    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    protected AnprClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         AgidJwtSignature agidJwtSignature,
                         AgidJwtTrackingEvidence agidJwtTrackingEvidence,
                         AnprSecretConfig anprSecretConfig,
                         AnprWebClient anprWebClient,
                         PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.agidJwtSignature = agidJwtSignature;
        this.agidJwtTrackingEvidence = agidJwtTrackingEvidence;
        this.anprSecretConfig = anprSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
        this.anprWebClient = anprWebClient;
    }

    public Mono<ResponseE002OKDto> callEService(E002RequestDto requestDto) {
        String agidTrackingEvidence = agidJwtTrackingEvidence.createAgidJwt();
        String auditAudience = createDigestFromAuditJws(agidTrackingEvidence);
        PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(anprSecretConfig.getPdndSecretName());
        pdndSecretValue.setAuditDigest(auditAudience);
        return accessTokenExpiringMap.getPDNDToken(pdndSecretValue.getJwtConfig().getPurposeId(), pdndSecretValue, true)
                .flatMap(tokenEntry -> callAnpr(requestDto, tokenEntry, agidTrackingEvidence))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ANPR_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<ResponseE002OKDto> callAnpr(E002RequestDto requestDto, AccessTokenCacheEntry tokenEntry, String agidTrackingEvidence) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.ANPR, PROCESS_SERVICE_ANPR_ADDRESS);
        String s = convertToJson(requestDto);
        String digest = createDigestFromPayload(s);
        log.debug("digest: {}", digest);
        WebClient webClient = anprWebClient.init();
        return webClient.post()
                .uri("/anpr-service-e002")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(tokenEntry.getTokenValue());
                    httpHeaders.add("Agid-JWT-Signature", agidJwtSignature.createAgidJwt(digest));
                    httpHeaders.add("Agid-JWT-TrackingEvidence", agidTrackingEvidence);
                    httpHeaders.add("Content-Encoding", "UTF-8");
                    httpHeaders.add("Digest", digest);
                    httpHeaders.add("bearerAuth", tokenEntry.getTokenValue());
                })
                .bodyValue(s)
                .retrieve()
                .bodyToMono(ResponseE002OKDto.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.ANPR, throwable.getMessage());
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        log.info("GovWay-Transaction-ID: {}", e.getHeaders().getFirst("GovWay-Transaction-ID"));
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), AnprResponseKO.class);
                    }
                });
    }

    private String convertToJson(E002RequestDto requestDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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

    private String createDigestFromAuditJws(String request) {
        try {
            byte[] digest =  MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR, e);
        }
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.debug("Try Retry call to ANPR");
            return true;
        }
        return false;
    }
}
