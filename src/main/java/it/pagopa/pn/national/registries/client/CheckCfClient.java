package it.pagopa.pn.national.registries.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CheckCfClient extends CommonBaseClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String basePath;
    private final String purposeId;

    public CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                      @Value("${pdnd.b001.purpose-id}") String purposeId,
                      @Value("${pdnd.agenzia-entrate.base-path}") String basePath) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.basePath=basePath;
        this.purposeId=purposeId;
    }

    public Mono<ApiClient> getApiClient(){
        return (accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry -> {
            log.info(accessTokenCacheEntry.getAccessToken());
            return createApiClient(accessTokenCacheEntry);
        }).doOnError(throwable -> log.error("error: ",throwable)));
    }

    private Mono<ApiClient> createApiClient(AccessTokenCacheEntry clientCredentialsResponseDto) {
        ApiClient apiClient = new ApiClient(initWebClient(it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.ApiClient.buildWebClientBuilder()).build());
        apiClient.setBearerToken(clientCredentialsResponseDto.getAccessToken());
        apiClient.setBasePath(basePath);
        log.info(clientCredentialsResponseDto.getAccessToken());
        return  Mono.just(apiClient);
    }

    protected WebClient.Builder initWebClient(WebClient.Builder builder){

        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        return super.enrichBuilder(builder.clientConnector(new ReactorClientHttpConnector(httpClient)));
    }
}
