package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.service.TokenProvider;
import it.pagopa.pn.national.registries.exceptions.PdndTokenGeneratorException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class AccessTokenExpiringMap {

    private final TokenProvider tokenProvider;

    protected ExpiringMap<String, AccessTokenCacheEntry> expiringMap = ExpiringMap.builder()
            .asyncExpirationListener((purposeId, accessTokenEntry) -> log.info("token for: {} is expired",purposeId))
            .variableExpiration()
            .build();


    public AccessTokenExpiringMap(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }


    public Mono<AccessTokenCacheEntry> getToken(String purposeId) {
        if (expiringMap.isEmpty() || !expiringMap.containsKey(purposeId)) {
            return requireNewAccessToken(purposeId);
        } else {
            long expiration = expiringMap.getExpectedExpiration(purposeId);
            if(expiration <= 120000) {
                return requireNewAccessToken(purposeId);
            }
            else {
                log.info("Existing Access Token Required with PurposeId: " + purposeId);
                return Mono.just(expiringMap.get(purposeId));
            }
        }
    }

    private Mono<AccessTokenCacheEntry> requireNewAccessToken(String purposeId) {
        log.info("New Access Token Required with PurposeId: " + purposeId);
        try {
            return tokenProvider.getToken(purposeId)
                    .map(clientCredentialsResponseDto -> {
                        AccessTokenCacheEntry tok = new AccessTokenCacheEntry(purposeId);
                        tok.setClientCredentials(clientCredentialsResponseDto);
                        expiringMap.put(purposeId, tok);
                        expiringMap.setExpiration(purposeId,clientCredentialsResponseDto.getExpiresIn(), TimeUnit.SECONDS);
                        return tok;
                    });
        } catch (Exception e) {
            log.error("error during retrieve PDND Token -> ",e);
            throw new PdndTokenGeneratorException(e);
        }
    }
}
