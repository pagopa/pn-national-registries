package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteType;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPRequest.SoapBody;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse.Envelope;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPResponse.SOAPResponseTemplate;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.InfoCamereLegalErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.xml.bind.JAXB;
import java.io.*;
import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Component
@Slf4j
public class AdELegalClient {

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final CheckCfSecretConfig checkCfSecretConfig;

    protected AdELegalClient(AccessTokenExpiringMap accessTokenExpiringMap,
                            AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP,
                            @Value("${pn.national.registries.pdnd.agenzia-entrate.purpose-id}") String purposeId,
                            ObjectMapper objectMapper,
                            CheckCfSecretConfig checkCfSecretConfig) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.purposeId = purposeId;
        webClient = agenziaEntrateWebClientSOAP.init();
        this.mapper = objectMapper;
        this.checkCfSecretConfig = checkCfSecretConfig;
    }

    private String marshaller(Object object) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(object, sw);
        return sw.toString();
    }
    private CheckValidityRappresentanteRespType unmarshaller(String string) {
        Envelope soapEnvelope = JAXB.unmarshal(new StringReader(string), Envelope.class);
        CheckValidityRappresentanteRespType c = new CheckValidityRappresentanteRespType();
        c.setCodiceRitorno(soapEnvelope.getBody().getCheckValidityRappresentanteRespType().getCodiceRitorno());
        c.setValido(soapEnvelope.getBody().getCheckValidityRappresentanteRespType().isValido());
        c.setDettaglioEsito(soapEnvelope.getBody().getCheckValidityRappresentanteRespType().getDettaglioEsito());
        return c;
    }
    public Mono<Object> getToken(){
        return Mono.just(new Object());
    }
    public Mono<CheckValidityRappresentanteRespType> checkTaxIdAndVatNumberAdE(CheckValidityRappresentanteType request)  {
        // HEADER DA COSTRUIRE O QUI O NEL COSTRUTTORE
      //  SoapEnvelopeRequest soapEnvelopeRequest = new SoapEnvelopeRequest("Header", request);
        return getToken().flatMap(accessTokenCacheEntry -> {
                    return webClient.post()
                            .uri("/legalerappresentateAdE/check")
                            .contentType(MediaType.TEXT_XML)
                            .body(Mono.just(marshaller(new Envelope())), String.class)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::unmarshaller)

                            .doOnError(throwable -> {
                                if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException) {
                                    WebClientResponseException ex = (WebClientResponseException) throwable;
                                    throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                            ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                            Charset.defaultCharset(), InfoCamereLegalErrorDto.class);
                                }
                            }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                            new PnInternalException(ERROR_MESSAGE_LEGALE_RAPPRESENTANTE, ERROR_CODE_LEGALE_RAPPRESENTANTE, retrySignal.failure())));
                }
        );
    }

    protected boolean checkExceptionType(Throwable throwable) {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException exception = (WebClientResponseException) throwable;
                return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
            }
            return false;
        }


}
