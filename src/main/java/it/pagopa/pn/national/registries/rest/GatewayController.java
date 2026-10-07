package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.AddressApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.PhysicalAddressesRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.PhysicalAddressesResponseDto;
import it.pagopa.pn.national.registries.service.GatewayService;
import it.pagopa.pn.national.registries.utils.RequestIdUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class GatewayController implements AddressApi {

    private final GatewayService gatewayService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;


    public GatewayController(GatewayService gatewayService, Scheduler scheduler) {
        this.gatewayService = gatewayService;
        this.scheduler = scheduler;
    }

    /**
     * POST /national-registries-private/{cx-type}/addresses : Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     * Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici o digitali per la PF o la PG indicata
     *
     * @param recipientType             Enum per indicare se la ricerca è effettuata per una PF o per una PG (required)
     * @param monoAddressRequestBodyDto (required)
     * @return OK (status code 200)
     * or Bad request (status code 400)
     * or Not Found (status code 404)
     * or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<AddressOKDto>> getAddresses(String recipientType, Mono<AddressRequestBodyDto> monoAddressRequestBodyDto, String pnNationalRegistriesCxId, final ServerWebExchange exchange) {
        var mono = monoAddressRequestBodyDto
                .doOnNext(requestBody -> RequestIdUtils.putRequestIdToMDC(requestBody.getFilter().getCorrelationId()))
                .flatMap(addressRequestBodyDto -> gatewayService.retrieveDigitalOrPhysicalAddressAsync(recipientType, pnNationalRegistriesCxId, addressRequestBodyDto))
                .map(s -> ResponseEntity.ok()
                        .header("X-Request-ID", s.getCorrelationId())
                        .body(s))
                .publishOn(scheduler);

        return MDCUtils.addMDCToContextAndExecute(mono);
    }

    /**
     * POST /national-registries-private/physical-addresses : Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici per la PF o la PG indicata
     * Questo servizio si occupa di smistare le richieste in ingresso al fine di fornire uno o più indirizzi fisici per la PF o la PG indicata
     *
     * @param physicalAddressesRequestBodyDto  (required)
     * @return OK (status code 200)
     *         or Bad request (status code 400)
     *         or Not Found (status code 404)
     *         or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<PhysicalAddressesResponseDto>> getPhysicalAddresses(Mono<PhysicalAddressesRequestBodyDto> physicalAddressesRequestBodyDto, final ServerWebExchange exchange) {
        var mono = physicalAddressesRequestBodyDto
                .doOnNext(requestBody -> RequestIdUtils.putRequestIdToMDC(requestBody.getCorrelationId()))
                .flatMap(gatewayService::retrieveSyncPhysicalAddresses)
                .map(s -> ResponseEntity.ok()
                        .header("X-Request-ID", s.getCorrelationId())
                        .body(s))
                .publishOn(scheduler);

        return MDCUtils.addMDCToContextAndExecute(mono);
    }

}
