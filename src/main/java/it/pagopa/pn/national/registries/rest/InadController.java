package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.DigitalAddressInadApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.service.InadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class InadController{

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
    public Mono<ResponseEntity<ResponseRequestDigitalAddress>> digitalAddressINAD(String recipientType, Mono<GetDigitalAddressINADRequestBodyDto> extractDigitalAddressINADRequestBodyDto, final ServerWebExchange exchange) {
        return extractDigitalAddressINADRequestBodyDto.flatMap(inadService::callEService)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
