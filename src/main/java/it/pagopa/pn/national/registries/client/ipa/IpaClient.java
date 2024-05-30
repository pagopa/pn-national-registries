package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.api.IpaApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_WS05_PEC;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_SERVICE_WS23_PEC;

@Component
@RequiredArgsConstructor
@lombok.CustomLog
public class IpaClient {

    private final IpaApi ipaApi;

    public Mono<WS23ResponseDto> callEServiceWS23(String taxId, String authId) {
        log.logInvokingExternalDownstreamService(PnLogger.EXTERNAL_SERVICES.IPA, PROCESS_SERVICE_WS23_PEC);
        return ipaApi.callEServiceWS23(taxId, authId)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.IPA, MaskDataUtils.maskInformation(throwable.getMessage()));
                    checkIPAException(throwable);
                });
    }

    public Mono<WS05ResponseDto> callEServiceWS05(String codAmm, String authId) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.IPA, PROCESS_SERVICE_WS05_PEC);
        return ipaApi.callEServiceWS05(codAmm, authId)
                .doOnError(throwable -> {
                    log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.IPA, MaskDataUtils.maskInformation(throwable.getMessage()));
                    checkIPAException(throwable);
                });
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
