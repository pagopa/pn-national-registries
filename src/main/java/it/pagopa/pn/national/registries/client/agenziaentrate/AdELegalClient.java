package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyFilterDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.xml.bind.JAXB;
import java.io.StringWriter;
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
    public String marshaller(Object object) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(object, sw);
        return sw.toString();
    }
    public Mono<String> checkTaxIdAndVatNumberAdE(ADELegalRequestBodyFilterDto request)  {

        return getToken().flatMap(token -> {
                    return webClient.post()
                            .uri("/legalerappresentateAdE/check")
                            .contentType(MediaType.TEXT_XML)
                            .bodyValue("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                                    "   <soapenv:Envelope " +
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
                                if (!checkExceptionType(throwable) && (throwable instanceof WebClientResponseException)) {
                                    WebClientResponseException ex = (WebClientResponseException) throwable;
                                    throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                            ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                            Charset.defaultCharset(), ADELegalErrorDto.class);
                                }
                            }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                            new PnInternalException(ERROR_MESSAGE_LEGALE_RAPPRESENTANTE, ERROR_CODE_LEGALE_RAPPRESENTANTE, retrySignal.failure())));
                }
        );
    }

    protected boolean checkExceptionType(Throwable throwable) {
            if (throwable instanceof WebClientResponseException exception) {
                return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
            }
            return false;
        }
}
