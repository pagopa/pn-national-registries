package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.IpaApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class IpaController{

    private final IpaService ipaService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    public IpaController(IpaService ipaService, Scheduler scheduler) {
        this.ipaService = ipaService;
        this.scheduler = scheduler;
    }


    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/ipa/pec",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<Object>> ipaPec(@Valid @RequestBody Mono<IPARequestBodyDto> ipARequestBodyDto, ServerWebExchange exchange) {
        return ipARequestBodyDto.flatMap(ipaService::getIpaPec)
            .map(t -> ResponseEntity.ok().body(t))
            .publishOn(scheduler);
    }
}
