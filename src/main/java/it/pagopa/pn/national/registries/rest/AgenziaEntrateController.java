package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.AgenziaEntrateApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class AgenziaEntrateController implements AgenziaEntrateApi {

    private final AgenziaEntrateService agenziaEntrateService;
    private final Scheduler scheduler;



    public AgenziaEntrateController(AgenziaEntrateService agenziaEntrateService, Scheduler scheduler) {
        this.agenziaEntrateService = agenziaEntrateService;
        this.scheduler = scheduler;

    }

    /**
     * POST /national-registries-private/agenzia-entrate/tax-id : Questo servizio ritorna la validità e l’esistenza di un dato codice fiscale descritta da un campo di ritorno booleano nell’oggetto json di response
     * Questo servizio ritorna la validità e l’esistenza di un dato codice fiscale descritta da un campo di ritorno booleano nell’oggetto json di response
     *
     * @param checkTaxIdRequestBodyDto Effettua la ricerca di un codice fiscale (required)
     * @return OK (status code 200)
     * or Bad request (status code 400)
     * or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<CheckTaxIdOKDto>> checkTaxId(Mono<CheckTaxIdRequestBodyDto> checkTaxIdRequestBodyDto, final ServerWebExchange exchange) {
        return checkTaxIdRequestBodyDto.flatMap(agenziaEntrateService::callEService)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }

    /**
     * POST /national-registries-private/agenzia-entrate/legal : Il servizio consente la verifica di corrispondenza fra il codice fiscale del rappresentante legale di un soggetto giuridico e il soggetto giuridico stesso.
     * Il servizio consente la verifica di corrispondenza fra il codice fiscale del rappresentante legale di un soggetto giuridico e il soggetto giuridico stesso.
     *
     * @param adELegalRequestBodyDto (required)
     * @return OK (status code 200)
     * or Unauthorized (status code 401)
     * or Internal server error (status code 500)
     * or Service Unavailable (status code 503)
     */

    @Override
    public Mono<ResponseEntity<ADELegalOKDto>> adeLegal(Mono<ADELegalRequestBodyDto> adELegalRequestBodyDto, final ServerWebExchange exchange) {
        return adELegalRequestBodyDto.flatMap(agenziaEntrateService::checkTaxIdAndVatNumber)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
