package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class InadClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final InadApiClient inadApiClient;

    protected InadClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         InadApiClient inadApiClient,
                         @Value("${pdnd.c001.purpose-id}") String purposeId) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.inadApiClient = inadApiClient;
        this.purposeId=purposeId;
    }

    public Mono<ApiClient> getApiClient(){
        return accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry -> {
            inadApiClient.setBearerToken(accessTokenCacheEntry.getAccessToken());
            return Mono.just(inadApiClient);
        });
    }
}
