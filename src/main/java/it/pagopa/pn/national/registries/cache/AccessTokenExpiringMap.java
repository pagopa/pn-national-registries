package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class AccessTokenExpiringMap {

    private final TokenProvider tokenProvider;

    public ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .expirationListener((purposeId, accessTokenEntry) -> {
                try {
                    requireNewAccessToken(String.valueOf(purposeId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .expiration(5, TimeUnit.MINUTES)
            .build();


    public AccessTokenExpiringMap(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }


    public Mono<AccessTokenCacheEntry> getToken(String purposeId) throws Exception {
        if(expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewAccessToken(purposeId);
        }
        else {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if((expiration - ZonedDateTime.now().toInstant().toEpochMilli()) <= 120000) {
                return requireNewAccessToken(purposeId);
            }
            else {
                log.info("Existing Access Token Required with PurposeId: " + purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        }
    }


    private Mono<AccessTokenCacheEntry> requireNewAccessToken(String purposeId) throws Exception {
        log.info("New Access Token Required with PurposeId: " + purposeId);

        return tokenProvider.getToken(purposeId)
                .map(clientCredentialsResponseDto -> {
                    AccessTokenCacheEntry tok = new AccessTokenCacheEntry(purposeId);
                    tok.setClientCredentials(clientCredentialsResponseDto);
                    if(expiringMap.containsKey(purposeId)) {
                        expiringMap.remove(purposeId);
                    }
                    return expiringMap.put(purposeId, tok);
                });
    }


    public boolean isExpired(String purposeId) {
        log.info("Required Expiration for Access Token with PurposeId: " + purposeId);

        return expiringMap.containsKey(purposeId) ? true : false;
    }
}
