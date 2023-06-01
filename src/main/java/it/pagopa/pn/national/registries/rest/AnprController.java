package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.AddressAnprApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_ANPR_ADDRESS;

@RestController
@lombok.CustomLog
public class AnprController implements AddressAnprApi {

    private final AnprService anprService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    private final ValidateTaxIdUtils validateTaxIdUtils;
    
    public AnprController(AnprService anprService, Scheduler scheduler, ValidateTaxIdUtils validateTaxIdUtils) {
        this.anprService = anprService;
        this.scheduler = scheduler;
        this.validateTaxIdUtils = validateTaxIdUtils;
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
    public Mono<ResponseEntity<GetAddressANPROKDto>> addressANPR(GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto, final ServerWebExchange exchange) {
        log.logStartingProcess(PROCESS_NAME_ANPR_ADDRESS);
        validateTaxIdUtils.validateTaxId(getAddressANPRRequestBodyDto.getFilter().getTaxId(), PROCESS_NAME_ANPR_ADDRESS);
        return anprService.getAddressANPR(getAddressANPRRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .doOnNext(checkTaxIdOKDtoResponseEntity -> log.logEndingProcess(PROCESS_NAME_ANPR_ADDRESS))
                .doOnError(throwable -> log.logEndingProcess(PROCESS_NAME_ANPR_ADDRESS,false,throwable.getMessage()))
                .publishOn(scheduler);
    }
}
