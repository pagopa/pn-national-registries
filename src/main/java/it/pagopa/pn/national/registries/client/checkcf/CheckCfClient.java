package it.pagopa.pn.national.registries.client.checkcf;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.exceptions.CheckCfException;
import it.pagopa.pn.national.registries.model.checkcf.Richiesta;
import it.pagopa.pn.national.registries.model.checkcf.VerificaCodiceFiscale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;

    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            CheckCfWebClient checkCfWebClient,
                            @Value("${pdnd.c001.purpose-id}") String purposeId) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.purposeId = purposeId;
        webClient = checkCfWebClient.init();
    }

    public Mono<VerificaCodiceFiscale> callEService(Richiesta richiesta) {
        return accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry ->
                webClient.post()
                        .uri("/verifica")
                        .headers(httpHeaders -> {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                            httpHeaders.setBearerAuth(accessTokenCacheEntry.getAccessToken());
                        })
                        .bodyValue(BodyInserters.fromValue(richiesta))
                        .retrieve()
                        .bodyToMono(VerificaCodiceFiscale.class)
                        .retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new CheckCfException(retrySignal.failure()))));
    }

    private boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
