package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.AddressApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.ApiUtil;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressesOKDto;
import it.pagopa.pn.national.registries.service.AddressService;
import it.pagopa.pn.national.registries.service.IniPecService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.validation.Valid;

@RestController
public class AddressController implements AddressApi {

    private AddressService addressService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    public AddressController(AddressService addressService, Scheduler scheduler) {
        this.addressService = addressService;
        this.scheduler = scheduler;
    }


    /**
     * POST /national-registries-private/{cx-type}/addresses : Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     * Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     *
     * @param cxType Enum per indicare se la ricerca è effettuata per una PF o per una PG (required)
     * @param addressRequestBodyDto  (required)
     * @return OK (status code 200)
     *         or Bad request (status code 400)
     *         or Not Found (status code 404)
     *         or Internal server error (status code 500)
     */
    public Mono<ResponseEntity<AddressesOKDto>> getAddresses(String cxType, AddressRequestBodyDto addressRequestBodyDto,  final ServerWebExchange exchange) {
        return addressService.retrieveDigitalOrPhysicalAddress(cxType,addressRequestBodyDto)
                .map(s -> ResponseEntity.ok().body(s))
                .publishOn(scheduler);
    }

}
