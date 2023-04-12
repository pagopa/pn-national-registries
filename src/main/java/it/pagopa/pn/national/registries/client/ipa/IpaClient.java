package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

@Slf4j
@Component
public class IpaClient {

    private final WebClient webClient;

    private final IpaSecretConfig ipaSecretConfig;

    protected IpaClient(IpaWebClient ipaWebClient, IpaSecretConfig ipaSecretConfig) {
        this.ipaSecretConfig = ipaSecretConfig;
        webClient = ipaWebClient.init();
    }

    public Mono<WS23ResponseDto> callEServiceWS23(String taxId) {
        return webClient.post()
                .uri("/ws/WS23DOMDIGCFServices/api/WS23_DOM_DIG_CF")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA))
                .body(BodyInserters.fromFormData(createRequestWS23(taxId)))
                .retrieve()
                .bodyToMono(WS23ResponseDto.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), IPAPecErrorDto.class);
                    }
                });
    }

    private MultiValueMap<String, String> createRequestWS23(String taxId) {
        LinkedMultiValueMap<String, String> requestWS23 = new LinkedMultiValueMap<>();
        String authId = ipaSecretConfig.getIpaSecret().getAuthId();
        requestWS23.add("CF", taxId);
        requestWS23.add("AUTH_ID", authId);
        return requestWS23;
    }

}
