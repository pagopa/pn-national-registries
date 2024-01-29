package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.pdnd.PdndResponseKO;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Collections;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_PDND_TOKEN;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSSAGE_PDND_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class PdndClient {

    private final WebClient webClient;

    protected PdndClient(PdndWebClient pdndWebClient) {
        webClient = pdndWebClient.init();
    }

    public Mono<ClientCredentialsResponseDto> createToken(String clientAssertion, String clientAssertionType, String grantType, String clientId) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("client_assertion", Collections.singletonList(clientAssertion));
        map.put("client_id", Collections.singletonList(clientId));
        map.put("client_assertion_type", Collections.singletonList(clientAssertionType));
        map.put("grant_type", Collections.singletonList(grantType));

        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.PDND, PROCESS_SERVICE_PDND_TOKEN);
        return webClient.post()
                .uri("/token.oauth2")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue(map)
                .retrieve()
                .bodyToMono(ClientCredentialsResponseDto.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.PDND, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (isUnauthorized(throwable)) {
                        throw new PnInternalException(ERROR_MESSSAGE_PDND_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, throwable);
                    }
                    if (throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), PdndResponseKO.class);
                    }
                });
    }

    private boolean isUnauthorized(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
