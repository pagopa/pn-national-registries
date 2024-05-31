package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.api.AuthApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.model.pdnd.PdndResponseKO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_PDND_TOKEN;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_UNAUTHORIZED;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSSAGE_PDND_UNAUTHORIZED;

@Component
@lombok.CustomLog
public class PdndClient {
    private final AuthApi authApi;

    protected PdndClient(AuthApi authApi) {
        this.authApi = authApi;
    }

    public Mono<ClientCredentialsResponse> createToken(String clientAssertion, String clientAssertionType, String grantType, String clientId) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.PDND, PROCESS_SERVICE_PDND_TOKEN);
        return authApi.createToken(clientAssertion, clientAssertionType, grantType, clientId)
                .doOnError(throwable -> {
                    String maskedErrorMessage = throwable.getMessage().replaceFirst("/extract/.*\\?", "/extract***?");
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.PDND, maskedErrorMessage);
                    if (isUnauthorized(throwable)) {
                        throw new PnInternalException(ERROR_MESSSAGE_PDND_UNAUTHORIZED, ERROR_CODE_UNAUTHORIZED, throwable);
                    }
                    if (throwable instanceof WebClientResponseException e) {
                        throw new PnNationalRegistriesException(maskedErrorMessage, e.getStatusCode().value(),
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
