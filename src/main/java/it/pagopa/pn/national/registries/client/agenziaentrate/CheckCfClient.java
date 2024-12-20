package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.adecheckcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdResponseKO;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_AGENZIA_ENTRATE_CHECK_TAX_ID;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
@lombok.CustomLog
public class CheckCfClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final VerificheApi verificheApi;
    private final CheckCfSecretConfig checkCfSecretConfig;


    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    protected CheckCfClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            VerificheApi verificheApi,
                            CheckCfSecretConfig checkCfSecretConfig,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.checkCfSecretConfig = checkCfSecretConfig;
        this.verificheApi = verificheApi;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public Mono<VerificaCodiceFiscale> callEService(Richiesta richiesta) {
        PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(checkCfSecretConfig.getPdndSecret());
        return accessTokenExpiringMap.getPDNDToken(pdndSecretValue.getJwtConfig().getPurposeId(), pdndSecretValue, false)
                .flatMap(tokenEntry -> callVerifica(richiesta, tokenEntry))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ADE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<VerificaCodiceFiscale> callVerifica(Richiesta request, AccessTokenCacheEntry tokenEntry) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.ADE, PROCESS_SERVICE_AGENZIA_ENTRATE_CHECK_TAX_ID);
        verificheApi.getApiClient().setBearerToken(tokenEntry.getTokenValue());
        return verificheApi.postVerificaCodiceFiscale(request)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.ADE, throwable.getMessage());
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
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
}
