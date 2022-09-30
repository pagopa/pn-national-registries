package it.pagopa.pn.national.registries.client.checkcf;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final CheckCfApiClient checkCfApiClient;

    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            CheckCfApiClient checkCfApiClient,
                            @Value("${pdnd.c001.purpose-id}") String purposeId) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.checkCfApiClient = checkCfApiClient;
        this.purposeId=purposeId;
    }

    public Mono<ApiClient> getApiClient(){
        return accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry -> {
            checkCfApiClient.setBearerToken(accessTokenCacheEntry.getAccessToken());
            return Mono.just(checkCfApiClient);
        });
    }
}
