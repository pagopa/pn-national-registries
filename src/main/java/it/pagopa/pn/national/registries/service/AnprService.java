package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.anpr.E002ServiceApiCustom;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.RichiestaE002Dto;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.TipoCriteriRicercaE002Dto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPRRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class AnprService{

    private final AddressAnprConverter addressAnprConverter;
    private final AnprClient anprClient;
    private final E002ServiceApiCustom e002ServiceApiCustom;

    public AnprService(AddressAnprConverter addressAnprConverter,
                       AnprClient anprClient,
                       E002ServiceApiCustom e002ServiceApiCustom) {
        this.anprClient = anprClient;
        this.addressAnprConverter = addressAnprConverter;
        this.e002ServiceApiCustom = e002ServiceApiCustom;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        return anprClient.getApiClient().flatMap(apiClient -> {
            e002ServiceApiCustom.setApiClient(apiClient);
            return callEService(e002ServiceApiCustom,request);
        }).retryWhen(Retry.max(1).filter(this::checkExceptionType));
    }

    private Mono<GetAddressANPROKDto> callEService(E002ServiceApiCustom e002ServiceApi, GetAddressANPRRequestBodyDto request) {
        log.info("call method e002 with cf: {}", request.getFilter().getTaxId());
        return (e002ServiceApi.e002(createRequest(request))
                .map(rispostaE002OKDto -> addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, request.getFilter().getTaxId())));
    }

    public RichiestaE002Dto createRequest(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiesta = new RichiestaE002Dto();
        TipoCriteriRicercaE002Dto criteriRicercaE002Dto = new TipoCriteriRicercaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);
        return richiesta;
    }

    private boolean checkExceptionType(Throwable throwable){
        if(throwable instanceof WebClientResponseException){
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED);
        }
        return false;
    }
}
