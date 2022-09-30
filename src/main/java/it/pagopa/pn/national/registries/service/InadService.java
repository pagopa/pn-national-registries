package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.EstrazioniPuntualiApiCustom;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.exceptions.InadException;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class InadService{

    private final EstrazioniPuntualiApiCustom estrazioniPuntualiApiCustom;
    private final InadClient inadClient;

    public InadService(EstrazioniPuntualiApiCustom estrazioniPuntualiApiCustom,
                       InadClient inadClient) {
        this.estrazioniPuntualiApiCustom = estrazioniPuntualiApiCustom;
        this.inadClient = inadClient;
    }

    public Mono<GetDigitalAddressINADOKDto> getDigitalAddress(GetDigitalAddressINADRequestBodyDto request) {
        return inadClient.getApiClient().flatMap(client -> {
            estrazioniPuntualiApiCustom.setApiClient(client);
            return callEService(estrazioniPuntualiApiCustom, request);
        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new InadException(retrySignal.failure())));
    }

    private Mono<GetDigitalAddressINADOKDto> callEService(ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi, GetDigitalAddressINADRequestBodyDto request) {
            return (apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                    .map(DigitalAddressInadConverter::mapToResponseOk));
    }

    private boolean checkExceptionType(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode()== HttpStatus.UNAUTHORIZED;
        }
        return false;
    }

}
