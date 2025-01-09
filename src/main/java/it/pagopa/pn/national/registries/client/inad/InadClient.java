package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import it.pagopa.pn.national.registries.utils.MaskTaxIdInPathUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.util.Optional;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_INAD_ADDRESS;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INAD_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class InadClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi;
    private final InadSecretConfig inadSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    protected InadClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi,
                         InadSecretConfig inadSecretConfig,
                         PnNationalRegistriesSecretService pnNationalRegistriesSecretService
    ) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.apiEstrazioniPuntualiApi = apiEstrazioniPuntualiApi;
        this.inadSecretConfig = inadSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public Mono<ResponseRequestDigitalAddress> callEService(String taxId, String practicalReference, PnAuditLogEvent logEvent) {
        PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(inadSecretConfig.getPdndSecret());
        return accessTokenExpiringMap.getPDNDToken(pdndSecretValue.getJwtConfig().getPurposeId(), pdndSecretValue, false, logEvent)
                .flatMap(tokenEntry -> callExtract(taxId, practicalReference, tokenEntry, logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                // don't pass retrySignal.failure() to PnInternalException as cause
                                // the common handler prints the stack trace that includes the called URL and for INAD in the URL is the taxId
                                new PnInternalException(ERROR_MESSAGE_INAD_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED))
                );
    }

    private Mono<ResponseRequestDigitalAddress> callExtract(String taxId, String practicalReference, AccessTokenCacheEntry tokenEntry, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INAD, PROCESS_SERVICE_INAD_ADDRESS);
        apiEstrazioniPuntualiApi.getApiClient().setBearerToken(tokenEntry.getTokenValue());
        return apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(taxId, practicalReference)
                .doOnError(throwable -> {
                    logEvent.generateFailure("Error calling INAD service").log();
                    String maskedErrorMessage = Optional.ofNullable(throwable.getMessage())
                            .map(MaskTaxIdInPathUtils::maskTaxIdInPath)
                            .orElse("Unknown error");
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INAD, maskedErrorMessage);
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(maskedErrorMessage, ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InadResponseKO.class);
                    }
                });
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.debug("Try Retry call to INAD");
            return true;
        }
        return false;
    }
}
