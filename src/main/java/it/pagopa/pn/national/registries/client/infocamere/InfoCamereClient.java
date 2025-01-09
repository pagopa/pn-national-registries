package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnLogger;
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
    private final ObjectMapper mapper;
    private static final String TRAKING_ID = "X-Tracking-trackingId";

    private final LegalRepresentationApi legalRepresentationApi;
    private final LegalRepresentativeApi legalRepresentativeApi;
    private final PecApi pecApi;
    private final SedeApi sedeApi;

    protected InfoCamereClient(@Value("${pn.national.registries.infocamere.client-id}") String clientId,
                               AccessTokenExpiringMap accessTokenExpiringMap,
                               ObjectMapper mapper,
                               LegalRepresentationApi legalRepresentationApi,
                               LegalRepresentativeApi legalRepresentativeApi,
                               PecApi pecApi,
                               SedeApi sedeApi
    ) {
        this.clientId = clientId;
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.mapper = mapper;

        this.legalRepresentationApi = legalRepresentationApi;
        this.legalRepresentativeApi = legalRepresentativeApi;
        this.pecApi = pecApi;
        this.sedeApi = sedeApi;
    }

    public Mono<IniPecBatchResponse> callEServiceRequestId(IniPecBatchRequest request, PnAuditLogEvent logEvent) {
        String requestJson = convertToJson(request);
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value(), logEvent)
                .flatMap(token -> callRichiestaElencoPec(requestJson, token.getTokenValue(), logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<IniPecBatchResponse> callRichiestaElencoPec(String body, String token, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Retrieving correlationId [INFOCAMERE]");

        var apiClient = pecApi.getApiClient();
        apiClient.setBearerToken(token);
        return pecApi.callRichiestaElencoPec(InipecScopeEnum.PEC.value(), body, clientId)
                .doOnError(handleErrorCall(logEvent));
    }

    public Mono<IniPecPollingResponse> callEServiceRequestPec(String correlationId, PnAuditLogEvent logEvent) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.PEC.value(), logEvent)
                .flatMap(token -> callGetElencoPec(correlationId, token.getTokenValue(), logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<IniPecPollingResponse> callGetElencoPec(String correlationId, String token, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, "Getting elencoPec InfoCamere for correlationId");

        ApiClient apiClient = pecApi.getApiClient();
        apiClient.setBearerToken(token);
        return pecApi.callGetElencoPec(correlationId, InipecScopeEnum.PEC.value(), clientId)
                .doOnError(handleErrorCall(logEvent));
    }

    public Mono<AddressRegistroImprese> getLegalAddress(String taxId, PnAuditLogEvent logEvent) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.SEDE.value(), logEvent)
                .flatMap(token -> callGetLegalAddress(taxId, token.getTokenValue(), logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED))
                );
    }

    private Mono<AddressRegistroImprese> callGetLegalAddress(String taxId, String token, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS);

        ApiClient apiClient = sedeApi.getApiClient();
        apiClient.setBearerToken(token);
        return sedeApi.getAddressByTaxId(taxId, InipecScopeEnum.SEDE.value(), clientId)
                .doOnError(handleErrorCall(logEvent));
    }

    public Mono<InfoCamereLegalInstituionsResponse> getLegalInstitutions(CheckTaxIdRequestBodyFilterDto filter, PnAuditLogEvent logEvent) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), logEvent)
                .flatMap(token -> callGetLegalInstitutions(filter.getTaxId(), token.getTokenValue(), logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    public Mono<InfoCamereLegalInstituionsResponse> callGetLegalInstitutions(String taxId, String token, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS);

        ApiClient apiClient = legalRepresentativeApi.getApiClient();
        apiClient.setBearerToken(token);
        return legalRepresentativeApi.getLegalRepresentativeListByTaxId(taxId, InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), clientId)
                .doOnError(handleErrorCall(logEvent));
    }

    public Mono<InfoCamereVerification> checkTaxIdAndVatNumberInfoCamere(InfoCamereLegalRequestBodyFilterDto filterDto, PnAuditLogEvent logEvent) {
        return accessTokenExpiringMap.getInfoCamereToken(InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), logEvent)
                .flatMap(token -> callCheckTaxId(filterDto, token.getTokenValue(), logEvent))
                .retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    private Mono<InfoCamereVerification> callCheckTaxId(InfoCamereLegalRequestBodyFilterDto filterDto, String token, PnAuditLogEvent logEvent) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_LEGAL);

        legalRepresentationApi.getApiClient().setBearerToken(token);
        return legalRepresentationApi.checkTaxIdForLegalRepresentation(filterDto.getVatNumber(), filterDto.getTaxId(), InipecScopeEnum.LEGALE_RAPPRESENTANTE.value(), clientId)
                .doOnError(handleErrorCall(logEvent));
    }

    protected boolean shouldRetry(Throwable throwable) {
        return isUnauthorized(throwable);
    }

    private @NotNull Consumer<Throwable> handleErrorCall(PnAuditLogEvent logEvent) {
        return throwable -> {
            logEvent.generateFailure("Can not retrieve physical address from Info Camere").log();
            String maskedErrorMessage = Optional.ofNullable(throwable.getMessage())
                    .map(MaskTaxIdInPathUtils::maskTaxIdInPath)
                    .orElse("Unknown error");
            log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, maskedErrorMessage);
            if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                log.info(TRAKING_ID + ": {}", e.getHeaders().getFirst(TRAKING_ID));
                throw new PnNationalRegistriesException(maskedErrorMessage, e.getStatusCode().value(),
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

    private String convertToJson(IniPecBatchRequest iniPecBatchRequest) {
        try {
            return mapper.writeValueAsString(iniPecBatchRequest);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_INIPEC, ERROR_CODE_INIPEC, e);
        }
    }
}
