package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.utils.XMLWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_AGENZIA_ENTRATE_CHECK_TAX_ID;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_AGENZIA_ENTRATE_LEGAL;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ADE_UNAUTHORIZED;

@lombok.CustomLog
@Component
public class AdELegalClient {

    private final AdELegalWebClient adELegalWebClient;
    private final XMLWriter xmlWriter;

    protected AdELegalClient(AdELegalWebClient adELegalWebClient,
                             XMLWriter xmlWriter) {
        this.adELegalWebClient = adELegalWebClient;
        this.xmlWriter = xmlWriter;
    }

    public Mono<String> checkTaxIdAndVatNumberAdE(ADELegalRequestBodyFilterDto request) {
        return callCheck(request);
    }

    private Mono<String> callCheck(ADELegalRequestBodyFilterDto request) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.ADE + "_legal", PROCESS_SERVICE_AGENZIA_ENTRATE_LEGAL);
        WebClient webClient = adELegalWebClient.init();
        return webClient.post()
                .uri("/SPCBooleanoRappWS/VerificaRappresentanteEnteService")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(xmlWriter.getEnvelope(request.getTaxId(), request.getVatNumber()))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.ADE, throwable.getMessage());
                    if (!shouldRetry(throwable) && throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), ADELegalErrorDto.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::shouldRetry)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_ADE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, retrySignal.failure()))
                );
    }

    protected boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
