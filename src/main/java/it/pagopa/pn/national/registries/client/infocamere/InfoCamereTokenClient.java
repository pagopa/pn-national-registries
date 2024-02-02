package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Optional;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_INFO_CAMERE_GET_TOKEN;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED;

@lombok.CustomLog
@Component
public class InfoCamereTokenClient {

    private final WebClient webClient;
    private final InfoCamereJwsGenerator infoCamereJwsGenerator;
    private final String clientId;

    private static final String CLIENT_ID = "client_id";
    private static final String TRAKING_ID = "X-Tracking-trackingId";



    protected InfoCamereTokenClient(InfoCamereWebClient infoCamereGetTokenWebClient,
                                    @Value("${pn.national.registries.infocamere.client-id}") String clientId,
                                    InfoCamereJwsGenerator infoCamereJwsGenerator) {
        this.clientId = clientId;
        this.infoCamereJwsGenerator = infoCamereJwsGenerator;
        webClient = infoCamereGetTokenWebClient.init();
    }

    public Mono<String> getToken(String scope) {
        String jws = infoCamereJwsGenerator.createAuthRest(scope);
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_GET_TOKEN);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/authentication")
                        .queryParamIfPresent(CLIENT_ID, Optional.ofNullable(clientId))
                        .build())
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .bodyValue(jws)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, MaskDataUtils.maskInformation(throwable.getMessage()));
                    if (isUnauthorized(throwable)) {
                        throw new PnInternalException(ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, throwable);
                    }
                    if (throwable instanceof WebClientResponseException e) {
                        log.info(TRAKING_ID + ": {}", e.getHeaders().getFirst(TRAKING_ID));
                        throw new PnNationalRegistriesException(e.getMessage(), e.getStatusCode().value(),
                                e.getStatusText(), e.getHeaders(), e.getResponseBodyAsByteArray(),
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                });
    }


    private boolean isUnauthorized(Throwable throwable) {
        if (throwable instanceof WebClientResponseException exception) {
            log.info(TRAKING_ID + ": {}", exception.getHeaders().getFirst(TRAKING_ID));
            return exception.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
