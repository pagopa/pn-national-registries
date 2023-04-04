package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_IPA;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_IPA;

@Component
@Slf4j
public class IpaClient {

    private final WebClient webClient;

    private final IpaSecretConfig ipaSecretConfig;

    protected IpaClient(IpaWebClient ipaWebClient, IpaSecretConfig ipaSecretConfig) {
        webClient = ipaWebClient.init();
        this.ipaSecretConfig = ipaSecretConfig;
    }

    public Mono<WS23ResponseDto> callEServiceWS23(String taxId) {
        return webClient.post()
                .uri("/ws/WS23DOMDIGCFServices/api/WS23_DOM_DIG_CF")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA))
                .body(BodyInserters.fromFormData(createRequestWS23(taxId)))
                .retrieve()
                .bodyToMono(WS23ResponseDto.class)
                .doOnError(throwable -> {
                    if (!checkExceptionType(throwable) && throwable instanceof WebClientResponseException ex) {
                        throw new PnNationalRegistriesException(ex.getMessage(), ex.getStatusCode().value(),
                                ex.getStatusText(), ex.getHeaders(), ex.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), IPAPecErrorDto.class);
                    }
                }).retryWhen(Retry.max(1)
                        .filter(this::checkExceptionType)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PnInternalException(ERROR_MESSAGE_IPA, ERROR_CODE_IPA, retrySignal.failure()))
                );
    }

    protected boolean checkExceptionType(Throwable throwable) {
        log.debug("Try Retry call to IpaClient");
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private MultiValueMap<String, String> createRequestWS23(String taxId) {
        LinkedMultiValueMap<String, String> requestWS23 = new LinkedMultiValueMap<>();
        String authId = ipaSecretConfig.getIpaSecret().getAuthId();
        requestWS23.add("CF", taxId);
        requestWS23.add("AUTH_ID", authId);
        return requestWS23;
    }

}
