package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.constant.InipecScopeEnum;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.LegalRepresentationApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.LegalRepresentativeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.PecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.SedeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.utils.MaskTaxIdInPathUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Consumer;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
@lombok.CustomLog
public class InfoCamereClient {
    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String clientId;
    private static final String TRAKING_ID = "X-Tracking-trackingId";

    private final LegalRepresentationApi legalRepresentationApi;
    private final LegalRepresentativeApi legalRepresentativeApi;
    private final SedeApi sedeApi;

    protected InfoCamereClient(@Value("${pn.national.registries.infocamere.client-id}") String clientId,
                               AccessTokenExpiringMap accessTokenExpiringMap,
                               LegalRepresentationApi legalRepresentationApi,
                               LegalRepresentativeApi legalRepresentativeApi,
                               SedeApi sedeApi
    ) {
        this.clientId = clientId;
        this.accessTokenExpiringMap = accessTokenExpiringMap;

        this.legalRepresentationApi = legalRepresentationApi;
        this.legalRepresentativeApi = legalRepresentativeApi;
        this.sedeApi = sedeApi;
    }

    private void logJwt(String token) {
        log.debug("Using jwt = {}", token);
    }

    public Mono<AddressRegistroImprese> getLegalAddress(String taxId) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.SEDE.value())
                .flatMap(token -> callGetLegalAddress(taxId, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED))
                );
    }

    private Mono<AddressRegistroImprese> callGetLegalAddress(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS);
        this.logJwt(token);

        ApiClient apiClient = sedeApi.getApiClient();
        apiClient.setBearerToken(token);
        return sedeApi.getAddressByTaxIdWithHttpInfo(taxId, InipecScopeEnum.SEDE.value(), clientId)
                .doOnNext(responseEntity -> {
                    String trackingId = responseEntity.getHeaders().getFirst(TRAKING_ID);
                    log.info("callGetLegalAddress - responded with tracking ID: {}", trackingId);
                })
                .map(ResponseEntity::getBody)
                .doOnError(handleErrorCall());
    }

    public Mono<InfoCamereLegalInstituionsResponse> getLegalInstitutions(CheckTaxIdRequestBodyFilterDto filter) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callGetLegalInstitutions(filter.getTaxId(), token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    public Mono<InfoCamereLegalInstituionsResponse> callGetLegalInstitutions(String taxId, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS);
        this.logJwt(token);

        ApiClient apiClient = legalRepresentativeApi.getApiClient();
        apiClient.setBearerToken(token);
        return legalRepresentativeApi.getLegalRepresentativeListByTaxIdWithHttpInfo(taxId, InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), clientId)
                .doOnNext(responseEntity -> {
                    String trackingId = responseEntity.getHeaders().getFirst(TRAKING_ID);
                    log.info("callGetLegalInstitutions - responded with tracking ID: {}", trackingId);
                })
                .map(ResponseEntity::getBody)
                .doOnError(handleErrorCall());
    }

    public Mono<InfoCamereVerification> checkTaxIdAndVatNumberInfoCamere(InfoCamereLegalRequestBodyFilterDto filterDto) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value())
                .flatMap(token -> callCheckTaxId(filterDto, token.getTokenValue()))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<InfoCamereVerification> callCheckTaxId(InfoCamereLegalRequestBodyFilterDto filterDto, String token) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL);
        this.logJwt(token);

        legalRepresentationApi.getApiClient().setBearerToken(token);
        return legalRepresentationApi.checkTaxIdForLegalRepresentationWithHttpInfo(filterDto.getVatNumber(), filterDto.getTaxId(), InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), clientId)
                .doOnNext(responseEntity -> {
                    String trackingId = responseEntity.getHeaders().getFirst(TRAKING_ID);
                    log.info("callCheckTaxId - responded with tracking ID: {}", trackingId);
                })
                .map(ResponseEntity::getBody)
                .doOnError(handleErrorCall());
    }

    protected boolean shouldRetry(Throwable throwable) {
        return isUnauthorized(throwable);
    }

    private @NotNull Consumer<Throwable> handleErrorCall() {
        return throwable -> {
            String maskedErrorMessage = Optional.ofNullable(throwable.getMessage())
                    .map(MaskTaxIdInPathUtils::maskTaxIdInPath)
                    .orElse("Unknown error");
            if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                log.info(TRAKING_ID + ": {}", e.getHeaders().getFirst(TRAKING_ID));
                String exceptionMessage = MaskTaxIdInPathUtils.maskTaxIdInPath(CommonBaseClient.elabExceptionMessage(e));
                log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, exceptionMessage, throwable);
                throw new PnNationalRegistriesException(maskedErrorMessage, e.getStatusCode().value(),
                        e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                        Charset.defaultCharset(), InfocamereResponseKO.class);
            } else {
                log.debug("Unhandled exception during call to InfoCamere", throwable);
                log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, maskedErrorMessage, throwable);
            }
        };
    }

    private boolean isUnauthorized(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
