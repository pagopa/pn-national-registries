package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.InfoCamereApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;


@RestController
@lombok.CustomLog
public class InfoCamereController implements InfoCamereApi {

    private final InfoCamereService infoCamereService;

    @Qualifier("nationalRegistriesScheduler")
    private final Scheduler scheduler;


    public InfoCamereController(InfoCamereService infoCamereService, Scheduler scheduler) {
        this.infoCamereService = infoCamereService;
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
    public Mono<ResponseEntity<GetDigitalAddressIniPECOKDto>> digitalAddressIniPEC(Mono<GetDigitalAddressIniPECRequestBodyDto> getDigitalAddressIniPECRequestBodyDto, String pnNationalRegistriesCxId,  final ServerWebExchange exchange) {
        return getDigitalAddressIniPECRequestBodyDto.flatMap(requestBody -> infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, requestBody, null))
                .map(t -> ResponseEntity.ok().body(t))
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
    public Mono<ResponseEntity<GetAddressRegistroImpreseOKDto>> addressRegistroImprese(Mono<GetAddressRegistroImpreseRequestBodyDto> getAddressRegistroImpreseRequestBodyDto, final ServerWebExchange exchange) {
        return getAddressRegistroImpreseRequestBodyDto.flatMap(infoCamereService::getRegistroImpreseLegalAddress)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }

    /**
     * POST /national-registries-private/infocamere/legal-institutions : Il servizio restituisce la lista delle imprese che hanno come legale rappresentante la persona fisica indicata.
     * Il servizio restituisce la lista delle imprese che hanno come legale rappresentante la persona fisica indicata.
     *
     * @param infoCamereLegalInstitutionsRequestBodyDto (required)
     * @return OK (status code 200)
     * or Bad request (status code 400)
     * or Unauthorized (status code 401)
     * or Not Found (status code 404)
     * or Internal server error (status code 500)
     * or Service Unavailable (status code 503)
     */
    @Override
    public Mono<ResponseEntity<InfoCamereLegalInstitutionsOKDto>> infoCamereLegalInstitutions(Mono<InfoCamereLegalInstitutionsRequestBodyDto> infoCamereLegalInstitutionsRequestBodyDto, final ServerWebExchange exchange) {
        return infoCamereLegalInstitutionsRequestBodyDto
                .flatMap(infoCamereService::getLegalInstitutions)
                .map(t -> ResponseEntity.ok().body(t))
                .publishOn(scheduler);
    }

}
