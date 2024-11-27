package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
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
public class AnprController {

    private final AnprService anprService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;


    
    public AnprController(AnprService anprService, Scheduler scheduler) {
        this.anprService = anprService;
        this.scheduler = scheduler;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/anpr/address",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<RispostaE002OK>> addressANPR(@Valid @RequestBody Mono<GetAddressANPRRequestBodyDto> getAddressANPRRequestBodyDto, final ServerWebExchange exchange) {
        return getAddressANPRRequestBodyDto.flatMap(anprService::getAddressANPR)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
