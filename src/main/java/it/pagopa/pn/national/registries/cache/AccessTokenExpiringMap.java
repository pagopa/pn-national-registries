package it.pagopa.pn.national.registries.cache;

import com.auth0.jwt.JWT;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Slf4j
@Component
public class AccessTokenExpiringMap {

    private final Integer pdndDeadline;
    private final Integer infoCamereDeadline;

    private final TokenProvider tokenProvider;

    protected ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .asyncExpirationListener((tokenKey, entry) -> log.info("token for key {} has expired", tokenKey))
            .variableExpiration()
            .build();

    public AccessTokenExpiringMap(TokenProvider tokenProvider,
                                  @Value("${pn.national-registries.pdnd.token.deadline}") Integer pdndDeadline,
                                  @Value("${pn.national.registries.infocamere.token.deadline}") Integer infoCamereDeadline) {
        this.tokenProvider = tokenProvider;
        this.pdndDeadline = pdndDeadline;
        this.infoCamereDeadline = infoCamereDeadline;
    }

    public Mono<AccessTokenCacheEntry> getPDNDToken(String purposeId, PdndSecretValue pdndSecretValue, boolean isAnpr, PnAuditLogEvent logEvent) {
        if (isAnpr || expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewPDNDAccessToken(purposeId, pdndSecretValue, logEvent);
        }
        try {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if (expiration <= pdndDeadline) {
                return requireNewPDNDAccessToken(purposeId, pdndSecretValue, logEvent);
            } else {
                log.info("Existing Access Token Required with purposeId: {}", purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        } catch (NoSuchElementException e) {
            return requireNewPDNDAccessToken(purposeId, pdndSecretValue, logEvent);
        }
    }

    public Mono<AccessTokenCacheEntry> getInfoCamereToken(String scope, PnAuditLogEvent logEvent) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(scope)) {
            return requireNewInfoCamereAccessToken(scope, logEvent);
        }
        try {
            long expiration = expiringMap.getExpectedExpiration(scope);
            if (expiration <= infoCamereDeadline) {
                return requireNewInfoCamereAccessToken(scope, logEvent);
            } else {
                log.info("Existing InfoCamere Access Token Required with scope: {}", scope);
                return Mono.just(expiringMap.get(scope));
            }
        } catch (NoSuchElementException e) {
            return requireNewInfoCamereAccessToken(scope, logEvent);
        }
    }

    private Mono<AccessTokenCacheEntry> requireNewPDNDAccessToken(String purposeId, PdndSecretValue pdndSecretValue, PnAuditLogEvent logEvent) {
        log.info("New PDND Access Token Required with purposeId: {}", purposeId);
        return tokenProvider.getTokenPdnd(pdndSecretValue, logEvent)
                .map(dto -> {
                    AccessTokenCacheEntry entry = new AccessTokenCacheEntry(purposeId);
                    entry.setClientCredentials(dto);
                    expiringMap.put(purposeId, entry);
                    expiringMap.setExpiration(purposeId, dto.getExpiresIn(), TimeUnit.SECONDS);
                    log.debug("New PDND Access Token with purposeId {} expires in {}s", purposeId, dto.getExpiresIn());
                    return entry;
                });
    }

    private Mono<AccessTokenCacheEntry> requireNewInfoCamereAccessToken(String scope, PnAuditLogEvent logEvent) {
        log.info("New InfoCamere Access Token Required with scope: {}", scope);
        return tokenProvider.getTokenInfoCamere(scope, logEvent)
                .map(token -> {
                    AccessTokenCacheEntry entry = new AccessTokenCacheEntry(scope);
                    entry.setClientCredentials(token);
                    expiringMap.put(scope, entry);
                    long duration = JWT.decode(token).getExpiresAt().getTime() - System.currentTimeMillis();
                    if (duration > 0) {
                        expiringMap.setExpiration(scope, duration / 1000L, TimeUnit.SECONDS);
                    } else {
                        throw new PnInternalException(ERROR_CODE_INFOCAMERE_TOKEN_DURATION, ERROR_MESSAGE_INFOCAMERE_TOKEN_DURATION);
                    }
                    log.debug("New InfoCamere Access Token with scope {} expires in {}ms", scope, duration);
                    return entry;
                });
    }
}
