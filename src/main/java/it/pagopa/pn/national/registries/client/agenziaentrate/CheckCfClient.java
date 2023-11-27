package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdResponseKO;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.util.List;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_AGENZIA_ENTRATE_CHECK_TAX_ID;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;
import static reactor.core.Exceptions.isRetryExhausted;

@Component
@lombok.CustomLog
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final CheckCfWebClient checkCfWebClient;
    private final CheckCfSecretConfig checkCfSecretConfig;

    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            CheckCfWebClient checkCfWebClient,
                            CheckCfSecretConfig checkCfSecretConfig,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.checkCfSecretConfig = checkCfSecretConfig;
        this.checkCfWebClient = checkCfWebClient;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public Mono<TaxIdVerification> callEService(Request richiesta) {
        PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(checkCfSecretConfig.getPurposeId(), checkCfSecretConfig.getPdndSecret());
        return accessTokenExpiringMap.getPDNDToken(checkCfSecretConfig.getPurposeId(), pdndSecretValue, false)
                .flatMap(tokenEntry -> callVerifica(richiesta, tokenEntry))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ADE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<TaxIdVerification> callVerifica(Request richiesta, AccessTokenCacheEntry tokenEntry) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.ADE, PROCESS_SERVICE_AGENZIA_ENTRATE_CHECK_TAX_ID);
        String s = convertToJson(richiesta);
        WebClient webClient = checkCfWebClient.init();
        return webClient.post()
                .uri("/verifica")
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    httpHeaders.setBearerAuth(tokenEntry.getTokenValue());
                })
                .bodyValue(s)
                .retrieve()
                .bodyToMono(TaxIdVerification.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.ADE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), TaxIdResponseKO.class);
                    }
                    if (isRetryExhausted(throwable) && throwable.getCause() instanceof WebClientResponseException.TooManyRequests e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), TaxIdResponseKO.class);
                    }
                });
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.debug("Try Retry call to CheckCf");
            return true;
        }
        return false;
    }

    private String convertToJson(Request richiesta) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(richiesta);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}
