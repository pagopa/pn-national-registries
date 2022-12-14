package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.service.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AccessTokenExpiringMap {

    private final Integer deadline;

    private final TokenProvider tokenProvider;

    protected ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .asyncExpirationListener((purposeId, accessTokenEntry) ->
                    log.info("token for {} has expired", purposeId))
            .variableExpiration()
            .build();


    public AccessTokenExpiringMap(TokenProvider tokenProvider, @Value("${pn.national-registries.pdnd.token.deadline}")Integer deadline) {
        this.tokenProvider = tokenProvider;
        this.deadline = deadline;
    }

    public Mono<AccessTokenCacheEntry> getToken(String purposeId, SecretValue secretValue) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewAccessToken(purposeId, secretValue);
        } else {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if (expiration <= deadline) {
                return requireNewAccessToken(purposeId, secretValue);
            } else {
                log.info("Existing Access Token Required with PurposeId: " + purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        }
    }

    private Mono<AccessTokenCacheEntry> requireNewAccessToken(String purposeId, SecretValue secretValue) {
        log.info("New Access Token Required with PurposeId: " + purposeId);
        return tokenProvider.getToken(secretValue)
                .map(clientCredentialsResponseDto -> {
                    AccessTokenCacheEntry tok = new AccessTokenCacheEntry(purposeId);
                    tok.setClientCredentials(clientCredentialsResponseDto);
                    expiringMap.put(purposeId, tok);
                    expiringMap.setExpiration(purposeId, clientCredentialsResponseDto.getExpiresIn(), TimeUnit.SECONDS);
                    return tok;
                });
    }
}
