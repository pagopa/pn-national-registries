package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiImpreseRappresentateElencoApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRecuperoElencoPecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRecuperoSedeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRichiestaElencoPecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Consumer;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class InfoCamereClient {
    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String clientId;
    private static final String TRAKING_ID = "X-Tracking-trackingId";
    private final ApiImpreseRappresentateElencoApi legalRepresentativeApi;
    private final ApiRecuperoElencoPecApi pecApi;
    private final ApiRecuperoSedeApi sedeApi;
    private final ApiRichiestaElencoPecApi richiestaElencoPecApi;

    protected InfoCamereClient(@Value("${pn.national.registries.infocamere.client-id}") String clientId,
                               AccessTokenExpiringMap accessTokenExpiringMap,
                               ApiImpreseRappresentateElencoApi legalRepresentativeApi,
                               ApiRecuperoElencoPecApi pecApi,
                               ApiRecuperoSedeApi sedeApi,
                               ApiRichiestaElencoPecApi richiestaElencoPecApi
    ) {
        this.clientId = clientId;
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.legalRepresentativeApi = legalRepresentativeApi;
        this.pecApi = pecApi;
        this.sedeApi = sedeApi;
        this.richiestaElencoPecApi = richiestaElencoPecApi;
    }

    public Mono<RichiestaElencoPec200Response> callEServiceRequestId(IniPecBatchRequest request) {
        RichiestaElencoPecRequest elencoPecRequest = convertToRichiestaElencoPecRequest(request);
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callRichiestaElencoPec(elencoPecRequest, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<RichiestaElencoPec200Response> callRichiestaElencoPec(RichiestaElencoPecRequest request, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Retrieving correlationId [INFOCAMERE]");

        var apiClient = richiestaElencoPecApi.getApiClient();
        apiClient.setBearerToken(token);
        return richiestaElencoPecApi.richiestaElencoPec(clientId, request)
                .doOnError(handleErrorCall());
    }

    public Mono<GetElencoPec200Response> callEServiceRequestPec(String correlationId) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value())
                .flatMap(token -> callGetElencoPec(correlationId, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<GetElencoPec200Response> callGetElencoPec(String correlationId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Getting elencoPec InfoCamere for correlationId");

        ApiClient apiClient = pecApi.getApiClient();
        apiClient.setBearerToken(token);
        return pecApi.getElencoPec(correlationId, clientId)
                .doOnError(handleErrorCall());
    }

    public Mono<RecuperoSedeImpresa200Response> getLegalAddress(String taxId) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.SEDE.value())
                .flatMap(token -> callGetLegalAddress(taxId, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<RecuperoSedeImpresa200Response> callGetLegalAddress(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS);

        ApiClient apiClient = sedeApi.getApiClient();
        apiClient.setBearerToken(token);
        return sedeApi.recuperoSedeImpresa(taxId, token)
                .doOnError(handleErrorCall());
    }

    public Mono<LegaleRappresentanteLista200Response> getLegalInstitutions(CheckTaxIdRequestBodyFilterDto filter) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callGetLegalInstitutions(filter.getTaxId(), token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    public Mono<LegaleRappresentanteLista200Response> callGetLegalInstitutions(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS);

        ApiClient apiClient = legalRepresentativeApi.getApiClient();
        apiClient.setBearerToken(token);
        return legalRepresentativeApi.legaleRappresentanteLista(taxId, clientId)
                .doOnError(handleErrorCall());
    }

    protected boolean shouldRetry(Throwable throwable) {
        return isUnauthorized(throwable);
    }

    private @NotNull Consumer<Throwable> handleErrorCall() {
        return throwable -> {
            log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, throwable.getMessage());
            if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                log.info(TRAKING_ID + ": {}", e.getHeaders().getFirst(TRAKING_ID));
                throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                        e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                        Charset.defaultCharset(), InfocamereResponseKO.class);
            }
        };
    }

    private boolean isUnauthorized(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private RichiestaElencoPecRequest convertToRichiestaElencoPecRequest(IniPecBatchRequest iniPecBatchRequest) {
        return Optional.ofNullable(iniPecBatchRequest).map(
                req -> {
                    RichiestaElencoPecRequest request = new RichiestaElencoPecRequest();
                    request.setElencoCf(req.getElencoCf().stream().map(iniPecCf -> {CodiceFiscaleElement cf = new CodiceFiscaleElement(); cf.setCf(iniPecCf.getCf()); return cf;}).toList());
                    request.setDataOraRichiesta(req.getDataOraRichiesta());
                    return request;
                }
        ).orElseGet(RichiestaElencoPecRequest::new);
    }
}
