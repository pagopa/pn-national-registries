package it.pagopa.pn.national.registries.client.agenziaentrate;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage.RequestEnvelope;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_LEGALE_RAPPRESENTANTE;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_LEGALE_RAPPRESENTANTE;

@Component
@Slf4j
public class AdELegalClient {

    private final WebClient webClient;

    protected AdELegalClient(AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP) {
        webClient = agenziaEntrateWebClientSOAP.init();
    }


    public Mono<Object> getToken(){
        return Mono.just(new Object());
    }

    public Mono<String> checkTaxIdAndVatNumberAdE(CheckValidityRappresentanteType request)  {
        return getToken().flatMap(token -> webClient.post()
                .uri("/legalerappresentateAdE/check")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(new RequestEnvelope(request))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), ADELegalErrorDto.class);
                    }
                }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_LEGALE_RAPPRESENTANTE, ERROR_CODE_LEGALE_RAPPRESENTANTE, retrySignal.failure())))
        );
    }

    protected boolean checkExceptionType(Throwable throwable) {
            if (throwable instanceof WebClientResponseException exception) {
                return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
            }
            return false;
        }
}
