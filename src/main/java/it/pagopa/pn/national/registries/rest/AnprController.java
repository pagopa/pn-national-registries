package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.api.ConsultaResidenzaAnprApi;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.ConsultaResidenzaANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class AnprController implements ConsultaResidenzaAnprApi {

    private final AnprService anprService;

    public AnprController(AnprService anprServicee) {
        this.anprService = anprServicee;
    }


    /**
     * POST /consultaResidenzaANPR : Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     * Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta
     *
     * @param consultaResidenzaANPRRequestBodyDto Il servizio viene invocato per ottenere la residenza presente in ANPR per un cittadino, alla data di riferimento della richiesta (required)
     * @return OK (status code 200)
     * or Caso d&#39;uso invalido (status code 400)
     * or Caso d&#39;uso non trovato (status code 404)
     * or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<Object>> consultaResidenzaANPR
    (Mono<ConsultaResidenzaANPRRequestBodyDto> consultaResidenzaANPRRequestBodyDto, final ServerWebExchange exchange){
        return anprService.getResidence(consultaResidenzaANPRRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t));
    }
}
