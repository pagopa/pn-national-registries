package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.AddressAnprApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class AnprController implements AddressAnprApi {

    private final AnprService anprService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;


    
    public AnprController(AnprService anprService, Scheduler scheduler) {
        this.anprService = anprService;
        this.scheduler = scheduler;
    }

    /**
     * POST /national-registries-private/anpr/address : Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     * Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     *
     * @param getAddressANPRRequestBodyDto Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta (required)
     * @return OK (status code 200)
     *         or Caso d&#39;uso invalido (status code 400)
     *         or Caso d&#39;uso non trovato (status code 404)
     *         or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<GetAddressANPROKDto>> addressANPR(Mono<GetAddressANPRRequestBodyDto> getAddressANPRRequestBodyDto, final ServerWebExchange exchange) {
        return getAddressANPRRequestBodyDto.flatMap(requestBody -> anprService.getAddressANPR(requestBody, null))
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
