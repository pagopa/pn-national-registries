package it.pagopa.pn.national.registries.cache;

import com.auth0.jwt.JWT;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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

    private final Integer deadline;

    private final TokenProvider tokenProvider;


    protected ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .asyncExpirationListener((tokenKey, entry) -> log.info("token for key {} has expired", tokenKey))
            .variableExpiration()
            .build();

    public AccessTokenExpiringMap(TokenProvider tokenProvider, @Value("${pn.national-registries.pdnd.token.deadline}") Integer deadline) {
        this.tokenProvider = tokenProvider;
        this.deadline = deadline;
    }

    public Mono<AccessTokenCacheEntry> getPDNDToken(String purposeId, PdndSecretValue pdndSecretValue) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewPDNDAccessToken(purposeId, pdndSecretValue);
        }
        try {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if (expiration <= deadline) {
                return requireNewPDNDAccessToken(purposeId, pdndSecretValue);
            } else {
                log.info("Existing Access Token Required with purposeId: {}", purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        } catch (NoSuchElementException e) {
            return requireNewPDNDAccessToken(purposeId, pdndSecretValue);
        }
    }

    public Mono<AccessTokenCacheEntry> getInfoCamereToken(String scope) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(scope)) {
            return requireNewInfoCamereAccessToken(scope);
        }
        try {
            long expiration = expiringMap.getExpectedExpiration(scope);
            if (expiration <= deadline) {
                return requireNewInfoCamereAccessToken(scope);
            } else {
                log.info("Existing InfoCamere Access Token Required with scope: {}", scope);
                return Mono.just(expiringMap.get(scope));
            }
        } catch (NoSuchElementException e) {
            return requireNewInfoCamereAccessToken(scope);
        }
    }


    private Mono<AccessTokenCacheEntry> requireNewPDNDAccessToken(String purposeId, PdndSecretValue pdndSecretValue) {
        log.info("New PDND Access Token Required with purposeId: {}", purposeId);
        return tokenProvider.getTokenPdnd(pdndSecretValue)
                .map(dto -> {
                    AccessTokenCacheEntry entry = new AccessTokenCacheEntry(purposeId);
                    entry.setClientCredentials(dto);
                    expiringMap.put(purposeId, entry);
                    expiringMap.setExpiration(purposeId, dto.getExpiresIn(), TimeUnit.SECONDS);
                    return entry;
                });
    }

    private Mono<AccessTokenCacheEntry> requireNewInfoCamereAccessToken(String scope) {
        log.info("New InfoCamere Access Token Required with scope: {}", scope);
        return tokenProvider.getTokenInfoCamere(scope)
                .map(token -> {
                    AccessTokenCacheEntry entry = new AccessTokenCacheEntry(scope);
                    entry.setClientCredentials(token);
                    expiringMap.put(scope, entry);
                    long duration = JWT.decode(token).getExpiresAt().getTime() - System.currentTimeMillis();
                    if(duration > 0)
                        expiringMap.setExpiration(scope, duration/1000L, TimeUnit.SECONDS);
                    else
                        throw new PnInternalException(ERROR_CODE_INFOCAMERE_TOKEN_DURATION, ERROR_MESSAGE_INFOCAMERE_TOKEN_DURATION);
                    return entry;
                });
    }
}
