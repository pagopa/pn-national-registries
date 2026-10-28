package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.InfoCamereApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import it.pagopa.pn.national.registries.utils.RequestIdUtils;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Objects;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.retrieveRecipientType;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfRequestedMetric;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetric;


@RestController
@lombok.CustomLog
public class InfoCamereController implements InfoCamereApi {

    private final InfoCamereService infoCamereService;

    public InfoCamereController(InfoCamereService infoCamereService) {
        this.infoCamereService = infoCamereService;
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
    public Mono<ResponseEntity<GetDigitalAddressIniPECOKDto>> digitalAddressIniPEC(Mono<GetDigitalAddressIniPECRequestBodyDto> getDigitalAddressIniPECRequestBodyDto, String pnNationalRegistriesCxId, final ServerWebExchange exchange) {
        return getDigitalAddressIniPECRequestBodyDto
                .flatMap(requestBody -> {
                    RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(requestBody.getFilter().getCorrelationId(), exchange);
                    var mono = infoCamereService.getIniPecDigitalAddress(
                                    pnNationalRegistriesCxId, requestBody, new Date(),
                                    retrieveRecipientType(requestBody.getFilter().getTaxId(), null))
                            .map(ResponseEntity::ok);

                    return MDCUtils.addMDCToContextAndExecute(mono);
                });
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
        log.info("Request received for addressRegistroImprese - MDC: {}", MDC.getCopyOfContextMap());
        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);
        var mono = getAddressRegistroImpreseRequestBodyDto
                .flatMap(infoCamereService::getRegistroImpreseLegalAddress)
                .doOnNext(responseDto -> {
                    logCfRequestedMetric(null, GatewayDownstreamService.REGISTRO_IMPRESE, 1);
                    if(Objects.nonNull(responseDto.getProfessionalAddress())){
                        logCfWithAddressMetric(
                                null,
                                GatewayDownstreamService.REGISTRO_IMPRESE,
                                RecipientType.PG,
                                null
                        );
                    }
                })
                .map(ResponseEntity::ok);

        return MDCUtils.addMDCToContextAndExecute(mono);
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
    public Mono<ResponseEntity<InfoCamereLegalOKDto>> infoCamereLegal(Mono<InfoCamereLegalRequestBodyDto> infoCamereLegalRequestBodyDto, final ServerWebExchange exchange) {
        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);
        var mono = infoCamereLegalRequestBodyDto
                .flatMap(infoCamereService::checkTaxIdAndVatNumber)
                .map(ResponseEntity::ok);

        return MDCUtils.addMDCToContextAndExecute(mono);
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
        RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(null, exchange);
        var mono = infoCamereLegalInstitutionsRequestBodyDto
                .flatMap(infoCamereService::getLegalInstitutions)
                .map(ResponseEntity::ok);

        return MDCUtils.addMDCToContextAndExecute(mono);
    }

}
