package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.CheckTaxIdApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.service.CheckCfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@Slf4j
public class CheckCfController implements CheckTaxIdApi {

    private final CheckCfService checkCfService;
    private final Scheduler scheduler;


    public CheckCfController(CheckCfService checkCfService, Scheduler scheduler) {
        this.checkCfService = checkCfService;
        this.scheduler = scheduler;
    }

    /**
     * POST /national-registries-private/agenzia-entrate/tax-id : Questo servizio ritorna la validità e l’esistenza di un dato codice fiscale descritta da un campo di ritorno booleano nell’oggetto json di response
     * Questo servizio ritorna la validità e l’esistenza di un dato codice fiscale descritta da un campo di ritorno booleano nell’oggetto json di response
     *
     * @param checkTaxIdRequestBodyDto Effettua la ricerca di un codice fiscale (required)
     * @return OK (status code 200)
     *         or Bad request (status code 400)
     *         or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<CheckTaxIdOKDto>> checkTaxId(CheckTaxIdRequestBodyDto checkTaxIdRequestBodyDto, final ServerWebExchange exchange) {
        return checkCfService.callEService(checkTaxIdRequestBodyDto)
                    .map(t -> ResponseEntity.ok().body(t)).publishOn(scheduler);
    }
}
