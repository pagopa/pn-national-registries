package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.api.InfoCamereApi;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@RestController
@lombok.CustomLog
public class InfoCamereController  implements InfoCamereApi {

    private final InfoCamereService infoCamereService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;

    private final ValidateTaxIdUtils validateTaxIdUtils;

    public InfoCamereController(InfoCamereService infoCamereService, Scheduler scheduler, ValidateTaxIdUtils validateTaxIdUtils) {
        this.infoCamereService = infoCamereService;
        this.scheduler = scheduler;
        this.validateTaxIdUtils = validateTaxIdUtils;
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
    public Mono<ResponseEntity<GetDigitalAddressIniPECOKDto>> digitalAddressIniPEC(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto, String pnNationalRegistriesCxId,  final ServerWebExchange exchange) {
        log.logStartingProcess(PROCESS_NAME_INIPEC_PEC);
        validateTaxIdUtils.validateTaxId(getDigitalAddressIniPECRequestBodyDto.getFilter().getTaxId(), PROCESS_NAME_INIPEC_PEC);
        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, getDigitalAddressIniPECRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .doOnNext(checkTaxIdOKDtoResponseEntity -> log.logEndingProcess(PROCESS_NAME_INIPEC_PEC))
                .doOnError(throwable -> log.logEndingProcess(PROCESS_NAME_INIPEC_PEC,false,throwable.getMessage()))
                .publishOn(scheduler);
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
    @Override
    public Mono<ResponseEntity<GetAddressRegistroImpreseOKDto>> addressRegistroImprese(GetAddressRegistroImpreseRequestBodyDto getAddressRegistroImpreseRequestBodyDto, final ServerWebExchange exchange) {
        log.logStartingProcess(PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS);
        validateTaxIdUtils.validateTaxId(getAddressRegistroImpreseRequestBodyDto.getFilter().getTaxId(), PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS);
        return infoCamereService.getRegistroImpreseLegalAddress(getAddressRegistroImpreseRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .doOnNext(checkTaxIdOKDtoResponseEntity -> log.logEndingProcess(PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS))
                .doOnError(throwable -> log.logEndingProcess(PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS,false,throwable.getMessage()))
                .publishOn(scheduler);
    }


    /**
     * POST /national-registries-private/infocamere/legal : Questo servizio Il servizio consente di verificare se il codice fiscale della persona risulta legale rappresentante dell’impresa passata come parametro.
     * Questo servizio Il servizio consente di verificare se il codice fiscale della persona risulta legale rappresentante dell’impresa passata come parametro.
     *
     * @param infoCamereLegalRequestBodyDto  (required)
     * @return OK (status code 200)
     *         or Bad request (status code 400)
     *         or Unauthorized (status code 401)
     *         or Not Found (status code 404)
     *         or Internal server error (status code 500)
     *         or Service Unavailable (status code 503)
     */

    @Override
    public Mono<ResponseEntity<InfoCamereLegalOKDto>> infoCamereLegal(InfoCamereLegalRequestBodyDto infoCamereLegalRequestBodyDto, final ServerWebExchange exchange) {
        log.logStartingProcess(PROCESS_NAME_INFO_CAMERE_LEGAL);
        validateTaxIdUtils.validateTaxId(infoCamereLegalRequestBodyDto.getFilter().getTaxId(),PROCESS_NAME_INFO_CAMERE_LEGAL);
        return infoCamereService.checkTaxIdAndVatNumber(infoCamereLegalRequestBodyDto)
                .map(t -> ResponseEntity.ok().body(t))
                .doOnNext(checkTaxIdOKDtoResponseEntity -> log.logEndingProcess(PROCESS_NAME_INFO_CAMERE_LEGAL))
                .doOnError(throwable -> log.logEndingProcess(PROCESS_NAME_INFO_CAMERE_LEGAL,false,throwable.getMessage()))
                .publishOn(scheduler);
    }

}
