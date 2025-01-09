package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.AuthenticationApi;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_INFO_CAMERE_GET_TOKEN;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INFOCAMERE_UNAUTHORIZED;

@lombok.CustomLog
@Component
public class InfoCamereTokenClient {

    private final InfoCamereJwsGenerator infoCamereJwsGenerator;
    private final String clientId;

    private final AuthenticationApi authenticationApi;

    private static final String TRAKING_ID = "X-Tracking-trackingId";


    protected InfoCamereTokenClient(@Value("${pn.national.registries.infocamere.client-id}") String clientId,
                                    InfoCamereJwsGenerator infoCamereJwsGenerator,
                                    AuthenticationApi authenticationApi) {
        this.clientId = clientId;
        this.infoCamereJwsGenerator = infoCamereJwsGenerator;
        this.authenticationApi = authenticationApi;
    }

    public Mono<String> getToken(String scope, PnAuditLogEvent logEvent) {
        String jws = infoCamereJwsGenerator.createAuthRest(scope);
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, PROCESS_SERVICE_INFO_CAMERE_GET_TOKEN);

        return authenticationApi.getToken(jws, clientId)
                .doOnError(throwable -> {
                    logEvent.generateFailure("Error calling Info Camere service").log();
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.INFO_CAMERE, throwable.getMessage());
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
