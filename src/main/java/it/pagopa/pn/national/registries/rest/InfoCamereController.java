package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalInstitutionsRequestBodyDto;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.validation.Valid;


@RestController
@lombok.CustomLog
public class InfoCamereController{

    private final InfoCamereService infoCamereService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;


    public InfoCamereController(InfoCamereService infoCamereService, Scheduler scheduler) {
        this.infoCamereService = infoCamereService;
        this.scheduler = scheduler;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/registro-imprese/address",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<AddressRegistroImprese>> addressRegistroImprese(@Valid @RequestBody Mono<GetAddressRegistroImpreseRequestBodyDto> getAddressRegistroImpreseRequestBodyDto, final ServerWebExchange exchange) {
        return getAddressRegistroImpreseRequestBodyDto.flatMap(infoCamereService::getRegistroImpreseLegalAddress)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/infocamere/legal-institutions",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<InfoCamereLegalInstituionsResponse>> infoCamereLegalInstitutions(   @Valid @RequestBody Mono<InfoCamereLegalInstitutionsRequestBodyDto> infoCamereLegalInstitutionsRequestBodyDto, final ServerWebExchange exchange) {
        return infoCamereLegalInstitutionsRequestBodyDto
                .flatMap(infoCamereService::getLegalInstitutions)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }

}
