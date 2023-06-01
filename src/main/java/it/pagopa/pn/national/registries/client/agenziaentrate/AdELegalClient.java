package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyFilterDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_AGENZIA_ENTRATE_LEGAL;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ADE_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class AdELegalClient {

    private final WebClient webClient;

    protected AdELegalClient(AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP) {
        webClient = agenziaEntrateWebClientSOAP.init();
    }

    public Mono<Object> getToken() {
        return Mono.just(new Object());
    }

    public Mono<String> checkTaxIdAndVatNumberAdE(ADELegalRequestBodyFilterDto request) {
        return getToken()
                .flatMap(token -> callCheck(request));
    }

    private Mono<String> callCheck(ADELegalRequestBodyFilterDto request) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PROCESS_SERVICE_AGENZIA_ENTRATE_LEGAL);
        return webClient.post()
                .uri("/legalerappresentateAdE/check")
                .contentType(MediaType.TEXT_XML)
                .bodyValue("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<soapenv:Envelope " +
                        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:anag=\"http://anagrafica.verifica.rappresentante.ente\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<checkValidityRappresentante xmlns=\"http://anagrafica.verifica.rappresentante.ente\">" +
                        "<cfRappresentante>" + request.getTaxId() + "</cfRappresentante>" +
                        "<cfEnte>" + request.getVatNumber() + "</cfEnte>" +
                        "</checkValidityRappresentante>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
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
