package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.AddressApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.service.GatewayService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class GatewayController implements AddressApi {

    private final GatewayService gatewayService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    private final ValidateTaxIdUtils validateTaxIdUtils;

    public GatewayController(GatewayService gatewayService, Scheduler scheduler, ValidateTaxIdUtils validateTaxIdUtils) {
        this.gatewayService = gatewayService;
        this.scheduler = scheduler;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    /**
     * POST /national-registries-private/{cx-type}/addresses : Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     * Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     *
     * @param recipientType         Enum per indicare se la ricerca è effettuata per una PF o per una PG (required)
     * @param addressRequestBodyDto (required)
     * @return OK (status code 200)
     * or Bad request (status code 400)
     * or Not Found (status code 404)
     * or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<AddressOKDto>> getAddresses(String recipientType, AddressRequestBodyDto addressRequestBodyDto, String pnNationalRegistriesCxId, final ServerWebExchange exchange) {
        validateTaxIdUtils.validateTaxId(addressRequestBodyDto.getFilter().getTaxId());
        return gatewayService.retrieveDigitalOrPhysicalAddressAsync(recipientType, pnNationalRegistriesCxId, addressRequestBodyDto)
                .map(s -> ResponseEntity.ok().body(s))
                .publishOn(scheduler);
    }

}
