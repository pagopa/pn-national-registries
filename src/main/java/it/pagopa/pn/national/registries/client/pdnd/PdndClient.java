package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.pdnd.PdndResponseKO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Collections;

@Slf4j
@Component
public class PdndClient {

    private final WebClient webClient;

    protected PdndClient(PdndWebClient pdndWebClient) {
        webClient = pdndWebClient.initWebClient();
    }

    public Mono<ClientCredentialsResponseDto> createToken(String clientAssertion, String clientAssertionType, String grantType, String clientId) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("client_assertion", Collections.singletonList(clientAssertion));
        map.put("client_id", Collections.singletonList(clientId));
        map.put("client_assertion_type", Collections.singletonList(clientAssertionType));
        map.put("grant_type", Collections.singletonList(grantType));

        return webClient.post()
                .uri("/token.oauth2")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue(map)
                .retrieve()
                .bodyToMono(ClientCredentialsResponseDto.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), PdndResponseKO.class);
                    }
                });
    }

}
