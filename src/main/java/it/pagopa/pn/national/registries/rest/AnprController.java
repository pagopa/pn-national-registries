package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.api.GetAddressAnprApi;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@Slf4j
public class AnprController implements GetAddressAnprApi {

    private final AnprService anprService;

    public AnprController(AnprService anprServicee) {
        this.anprService = anprServicee;
    }

    /**
     * POST /getAddressANPR : Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     * Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     *
     * @param getAddressANPRRequestBodyDto Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta (required)
     * @return OK (status code 200)
     * or Caso d&#39;uso invalido (status code 400)
     * or Caso d&#39;uso non trovato (status code 404)
     * or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<GetAddressANPROKDto>> getAddressANPR(GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto, final ServerWebExchange exchange) {
        log.info("start method getAddressANPR");
        return anprService.getAddressANPR(getAddressANPRRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t)).publishOn(Schedulers.boundedElastic());
    }
}
