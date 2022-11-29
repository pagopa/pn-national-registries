package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.AddressRegistroImpreseApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.service.RegistroImpreseService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class RegistroImpreseController implements AddressRegistroImpreseApi {

    private final RegistroImpreseService registroImpreseService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    public RegistroImpreseController(RegistroImpreseService registroImpreseService, Scheduler scheduler) {
        this.registroImpreseService = registroImpreseService;
        this.scheduler = scheduler;
    }

    /**
     * POST /national-registries-private/registro-imprese/address : Consente di ottenere l’indirizzo della sede legale a cui corrisponde il codice fiscale al momento della consultazione
     * Consente di ottenere l’indirizzo della sede legale a cui corrisponde il codice fiscale al momento della consultazione
     *
     * @param getAddressRegistroImpreseRequestBodyDto Consente di ottenere l’indirizzo della sede legale a cui corrisponde il codice fiscale al momento della consultazione (required)
     * @return OK (status code 200)
     *         or Caso d&#39;uso invalido (status code 400)
     *         or Unauthorized (status code 401)
     *         or Caso d&#39;uso non trovato (status code 404)
     *         or Internal server error (status code 500)
     *         or Service Unavailable (status code 503)
     */

    public Mono<ResponseEntity<GetAddressRegistroImpreseOKDto>> addressRegistroImprese(GetAddressRegistroImpreseRequestBodyDto getAddressRegistroImpreseRequestBodyDto,  final ServerWebExchange exchange) {
        return registroImpreseService.getAddress(getAddressRegistroImpreseRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t)).publishOn(scheduler);
    }

}
