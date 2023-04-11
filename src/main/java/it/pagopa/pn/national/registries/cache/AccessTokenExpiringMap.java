package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AccessTokenExpiringMap {

    private final Integer deadline;

    private final TokenProvider tokenProvider;

    protected ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .asyncExpirationListener((purposeId, entry) -> log.info("token for purposeId {} has expired", purposeId))
            .variableExpiration()
            .build();

    public AccessTokenExpiringMap(TokenProvider tokenProvider, @Value("${pn.national-registries.pdnd.token.deadline}") Integer deadline) {
        this.tokenProvider = tokenProvider;
        this.deadline = deadline;
    }

    public Mono<AccessTokenCacheEntry> getToken(String purposeId, PdndSecretValue pdndSecretValue) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewAccessToken(purposeId, pdndSecretValue);
        }
        try {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if (expiration <= deadline) {
                return requireNewAccessToken(purposeId, pdndSecretValue);
            } else {
                log.info("Existing Access Token Required with purposeId: {}", purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        } catch (NoSuchElementException e) {
            return requireNewAccessToken(purposeId, pdndSecretValue);
        }
    }

    private Mono<AccessTokenCacheEntry> requireNewAccessToken(String purposeId, PdndSecretValue pdndSecretValue) {
        log.info("New Access Token Required with purposeId: {}", purposeId);
        return tokenProvider.getTokenPdnd(pdndSecretValue)
                .map(dto -> {
                    AccessTokenCacheEntry entry = new AccessTokenCacheEntry(purposeId);
                    entry.setClientCredentials(dto);
                    expiringMap.put(purposeId, entry);
                    expiringMap.setExpiration(purposeId, dto.getExpiresIn(), TimeUnit.SECONDS);
                    return entry;
                });
    }
}
