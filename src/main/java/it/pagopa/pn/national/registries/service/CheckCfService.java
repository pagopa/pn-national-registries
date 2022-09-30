package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.checkcf.CheckCfClient;
import it.pagopa.pn.national.registries.exceptions.CheckCfException;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.converter.CheckCfConverter;
import it.pagopa.pn.national.registries.client.checkcf.VerificheApiCustom;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class CheckCfService {

    private final CheckCfConverter checkCfConverter;
    private final VerificheApiCustom verificheApiCustom;
    private final CheckCfClient checkCfClient;

    public CheckCfService(CheckCfConverter checkCfConverter,
                          CheckCfClient checkCfClient,
                          VerificheApiCustom verificheApiCustom) {
        this.verificheApiCustom = verificheApiCustom;
        this.checkCfClient = checkCfClient;
        this.checkCfConverter = checkCfConverter;
    }

    public Mono<CheckTaxIdOKDto> getCfStatus(CheckTaxIdRequestBodyDto request) {
        return checkCfClient.getApiClient().flatMap(client -> {
            verificheApiCustom.setApiClient(client);
            return callEService(verificheApiCustom,request);
        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new CheckCfException(retrySignal.failure())));
    }

    private Mono<CheckTaxIdOKDto> callEService(VerificheApiCustom verificheApiCustom, CheckTaxIdRequestBodyDto request) {
        return verificheApiCustom.postVerificaCodiceFiscale(createRequest(request))
                .map(checkCfConverter::convertToCfStatusDto);
    }

    private boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode()==HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

    private Richiesta createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
    }
}
