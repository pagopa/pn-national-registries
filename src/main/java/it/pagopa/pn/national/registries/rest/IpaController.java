package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.IpaApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class IpaController implements IpaApi {

    private final IpaService ipaService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    private final ValidateTaxIdUtils validateTaxIdUtils;

    public IpaController(IpaService ipaService, Scheduler scheduler, ValidateTaxIdUtils validateTaxIdUtils) {
        this.ipaService = ipaService;
        this.scheduler = scheduler;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    /**
     * POST /national-registries-private/ipa/pec : Il servizio consente di individuare i domicili digitali associati ad un codice fiscale di un Ente o al codice fiscale di un suo servizio di fatturazione elettronica.
     * Il servizio consente di individuare i domicili digitali associati ad un codice fiscale di un Ente o al codice fiscale di un suo servizio di fatturazione elettronica.
     *
     * @param ipARequestBodyDto  (required)
     * @return OK (status code 200)
     *         or Unauthorized (status code 401)
     *         or Internal server error (status code 500)
     *         or Service Unavailable (status code 503)
    */
    @Override
    public Mono<ResponseEntity<IPAPecOKDto>> ipaPec(IPARequestBodyDto ipARequestBodyDto, ServerWebExchange exchange) {
        validateTaxIdUtils.validateTaxId(ipARequestBodyDto.getFilter().getTaxId());
        return ipaService.getIpaPec(ipARequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }


}
