package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.DigitalAddressIniPecApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.service.IniPecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;

@RestController
@Slf4j
public class IniPecController implements DigitalAddressIniPecApi {

    private final IniPecService iniPecService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    public IniPecController(IniPecService iniPecService, Scheduler scheduler) {
        this.iniPecService = iniPecService;
        this.scheduler = scheduler;
    }

    /**
     * POST /national-registries-private/inipec/digital-address : Consente di ottenere la PEC dell’impresa oppure del professionista corrispondente al codice fiscale al momento della consultazione. In caso di impresa si restituisce anche l’indirizzo della sede legale.
     * Consente di ottenere la PEC dell’impresa oppure del professionista corrispondente al codice fiscale al momento della consultazione. In caso di impresa si restituisce anche l’indirizzo della sede legale.
     *
     * @param getDigitalAddressIniPECRequestBodyDto Consente di ottenere la PEC dell’impresa oppure del professionista corrispondente al codice fiscale al momento della consultazione. In caso di impresa si restituisce anche l’indirizzo della sede legale. (required)
     * @return OK (status code 200)
     *         or Bad Request (status code 400)
     *         or Unauthorized (status code 401)
     *         or Not found (status code 404)
     *         or Internal server error (status code 500)
     *         or Service Unavailable (status code 503)
     */
    @Override
    public Mono<ResponseEntity<GetDigitalAddressIniPECOKDto>> digitalAddressIniPEC(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto, final ServerWebExchange exchange) {
        return iniPecService.getDigitalAddress(getDigitalAddressIniPECRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t)).publishOn(scheduler);
    }

    @PostMapping(
            value = "/national-registries-private/inipec/get",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public Mono<ResponseEntity<BatchPolling>> get(){
        return iniPecService.get()
                .map(t -> ResponseEntity.ok().body(t)).publishOn(scheduler);
    }


}
