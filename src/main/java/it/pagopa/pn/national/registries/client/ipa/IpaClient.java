package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.model.ipa.WS05ResponseDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_WS05_PEC;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_WS23_PEC;

@Component
@lombok.CustomLog
public class IpaClient {

    private final WebClient webClient;

    private final IpaSecretConfig ipaSecretConfig;

    protected IpaClient(IpaWebClient ipaWebClient, IpaSecretConfig ipaSecretConfig) {
        this.ipaSecretConfig = ipaSecretConfig;
        webClient = ipaWebClient.init();
    }

    public Mono<WS23ResponseDto> callEServiceWS23(String taxId) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PROCESS_SERVICE_WS23_PEC);
        return webClient.post()
                .uri("/ws/WS23DOMDIGCFServices/api/WS23_DOM_DIG_CF")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA))
                .body(BodyInserters.fromFormData(createRequestWS23(taxId)))
                .retrieve()
                .bodyToMono(WS23ResponseDto.class)
                .doOnError(this::checkIPAException);
    }


    private MultiValueMap<String, String> createRequestWS23(String taxId) {
        LinkedMultiValueMap<String, String> requestWS23 = new LinkedMultiValueMap<>();
        String authId = ipaSecretConfig.getIpaSecret().getAuthId();
        requestWS23.add("CF", taxId);
        requestWS23.add("AUTH_ID", authId);
        return requestWS23;
    }

    public Mono<WS05ResponseDto> callEServiceWS05(String codAmm) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PROCESS_SERVICE_WS05_PEC);
        return webClient.post()
                .uri("ws/WS05AMMServices/api/WS05_AMM")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA))
                .body(BodyInserters.fromFormData(createRequestWS05(codAmm)))
                .retrieve()
                .bodyToMono(WS05ResponseDto.class)
                .doOnError(this::checkIPAException);
    }

    private MultiValueMap<String, String> createRequestWS05(String codAmm) {
        LinkedMultiValueMap<String, String> requestWS05 = new LinkedMultiValueMap<>();
        String authId = ipaSecretConfig.getIpaSecret().getAuthId();
        requestWS05.add("COD_AMM", codAmm);
        requestWS05.add("AUTH_ID", authId);
        return requestWS05;
    }

    private void checkIPAException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            log.error("Error calling IPA service", e);
            throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                    e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
    }
}
