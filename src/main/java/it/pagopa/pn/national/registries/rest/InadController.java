package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.service.InadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.validation.Valid;

@RestController
@lombok.CustomLog
public class InadController  {

    private final InadService inadService;
    private final Scheduler scheduler;



    public InadController(InadService inadService, Scheduler scheduler) {
        this.inadService = inadService;
        this.scheduler = scheduler;
    }


    @RequestMapping(
            method = RequestMethod.POST,
            value = "/national-registries-private/{recipient-type}/inad/digital-address",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<ResponseRequestDigitalAddress>> digitalAddressINAD(   @PathVariable("recipient-type") String recipientType,
                @Valid @RequestBody Mono<GetDigitalAddressINADRequestBodyDto> getDigitalAddressINADRequestBodyDto,
        final ServerWebExchange exchange) {
        return getDigitalAddressINADRequestBodyDto.flatMap(request -> inadService.callEService(request, recipientType))
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
