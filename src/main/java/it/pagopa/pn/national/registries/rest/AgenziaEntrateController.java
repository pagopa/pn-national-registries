package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
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
public class AgenziaEntrateController {

    private final AgenziaEntrateService agenziaEntrateService;
    private final Scheduler scheduler;



    public AgenziaEntrateController(AgenziaEntrateService agenziaEntrateService, Scheduler scheduler) {
        this.agenziaEntrateService = agenziaEntrateService;
        this.scheduler = scheduler;

    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/agenzia-entrate/legal",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public  Mono<ResponseEntity<CheckValidityRappresentanteResp>> adeLegal(  @Valid @RequestBody Mono<ADELegalRequestBodyDto> adELegalRequestBodyDto, final ServerWebExchange exchange) {
        return adELegalRequestBodyDto.flatMap(agenziaEntrateService::checkTaxIdAndVatNumber)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
