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
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RichiestaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.xml.bind.DatatypeConverter;
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
    private final AgidJwtSignature agidJwtSignature;
    private final E002ServiceApi e002ServiceApi;
    private final AgidJwtTrackingEvidence agidJwtTrackingEvidence;
    private final AnprSecretConfig anprSecretConfig;

    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    protected AnprClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         AgidJwtSignature agidJwtSignature,
                         E002ServiceApi e002ServiceApi,
                         AgidJwtTrackingEvidence agidJwtTrackingEvidence,
                         AnprSecretConfig anprSecretConfig,
                         PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.agidJwtSignature = agidJwtSignature;
        this.e002ServiceApi = e002ServiceApi;
        this.agidJwtTrackingEvidence = agidJwtTrackingEvidence;
        this.anprSecretConfig = anprSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    @PostConstruct
    public void init() {
        e002ServiceApi.getApiClient().addDefaultHeader("Content-Encoding", "UTF-8");
    }

    public Mono<RispostaE002OK> callEService(RichiestaE002 requestDto) {
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

    private Mono<RispostaE002OK> callAnpr(RichiestaE002 request, AccessTokenCacheEntry tokenEntry, String agidTrackingEvidence) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.ANPR, PROCESS_SERVICE_ANPR_ADDRESS);
        String s = convertToJson(request);
        String digest = createDigestFromPayload(s);
        log.debug("digest: {}", digest);
        var bearerToken = "Bearer " + tokenEntry.getTokenValue();
        var agidJWTSignature = agidJwtSignature.createAgidJwt(digest);
        var bearerAuth = tokenEntry.getTokenValue();
        return e002ServiceApi.e002(request, bearerToken, agidJWTSignature, agidTrackingEvidence, bearerAuth, digest)
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

    private String convertToJson(RichiestaE002 requestDto) {
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
