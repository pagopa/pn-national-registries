package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AnprClient{

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final AnprApiClient anprApiClient;

    protected AnprClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         AnprApiClient anprApiClient,
                         @Value("${pdnd.c001.purpose-id}") String purposeId) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.anprApiClient = anprApiClient;
        this.purposeId=purposeId;
    }

    public Mono<ApiClient> getApiClient(){
        return accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry -> {
            log.info(accessTokenCacheEntry.getAccessToken());
            anprApiClient.setBearerToken(accessTokenCacheEntry.getAccessToken());
            return Mono.just(anprApiClient);
        });
    }
}
