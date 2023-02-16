package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.DigitalAddressInadApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.service.InadService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class InadController implements DigitalAddressInadApi {

    private final InadService inadService;
    private final Scheduler scheduler;

    private final ValidateTaxIdUtils validateTaxIdUtils;

    public InadController(InadService inadService, Scheduler scheduler, ValidateTaxIdUtils validateTaxIdUtils) {
        this.inadService = inadService;
        this.scheduler = scheduler;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    /**
     * POST /national-registries-private/inad/digital-address : Consente di ottenere il domicilio digitale corrispondente al codice fiscale al momento della consultazione e, in caso di domicilio digitale eletto in qualità di Professionista, anche l&#39;attività professionale esercitata.
     * Consente di ottenere il domicilio digitale corrispondente al codice fiscale al momento della consultazione e, in caso di domicilio digitale eletto in qualità di Professionista, anche l&#39;attività professionale esercitata.
     *
     * @param extractDigitalAddressINADRequestBodyDto Consente di ottenere il domicilio digitale corrispondente al codice fiscale al momento della consultazione e, in caso di domicilio digitale eletto in qualità di Professionista, anche l&#39;attività professionale esercitata. (required)
     * @return OK (status code 200)
     *         or Bad Request (status code 400)
     *         or Unauthorized (status code 401)
     *         or Forbidden (status code 403)
     *         or Not found (status code 404)
     *         or Internal server error (status code 500)
     *         or Service Unavailable (status code 503)
     */
    @Override
    public Mono<ResponseEntity<GetDigitalAddressINADOKDto>> digitalAddressINAD(GetDigitalAddressINADRequestBodyDto extractDigitalAddressINADRequestBodyDto, final ServerWebExchange exchange) {
        validateTaxIdUtils.validateTaxId(extractDigitalAddressINADRequestBodyDto.getFilter().getTaxId());
        return inadService.callEService(extractDigitalAddressINADRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }
}
