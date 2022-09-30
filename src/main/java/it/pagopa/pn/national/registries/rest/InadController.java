package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.GetDigitalAddressInadApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.service.InadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class InadController implements GetDigitalAddressInadApi {

    private final InadService inadService;
    private final Scheduler scheduler;

    public InadController(InadService inadServicee,Scheduler scheduler) {
        this.inadService = inadServicee;
        this.scheduler = scheduler;
    }

    /**
     * POST /extractDigitalAddress
     *
     * @param extractDigitalAddressINADRequestBodyDto Effettua la ricerca dei Domicili Digitali di uno specifico Codice Fiscale (required)
     * @return JSON di risposta che restituisce il domicilio digitale estratto. Per domicilio digitale eletto in qualità di Professionista è estratta anche l&#39;attività professionale esercitata. (status code 200)
     *         or Bad request (status code 400)
     *         or Internal server error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<GetDigitalAddressINADOKDto>> getDigitalAddressINAD(GetDigitalAddressINADRequestBodyDto extractDigitalAddressINADRequestBodyDto, final ServerWebExchange exchange) {
        return inadService.getDigitalAddress(extractDigitalAddressINADRequestBodyDto).map(t -> ResponseEntity.ok().body(t)).publishOn(scheduler);
    }
}
