package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.DigitalAddressInadApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.service.InadService;
import it.pagopa.pn.national.registries.utils.RequestIdUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_INAD_ADDRESS;

@RestController
@lombok.CustomLog
@RequiredArgsConstructor
public class InadController implements DigitalAddressInadApi {

    private final InadService inadService;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    /**
     * POST /national-registries-private/{recipient-type}/inad/digital-address : Consente di ottenere il domicilio digitale corrispondente al codice fiscale al momento della consultazione e, in caso di domicilio digitale eletto in qualità di Professionista, anche l&#39;attività professionale esercitata.
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
    public Mono<ResponseEntity<GetDigitalAddressINADOKDto>> digitalAddressINAD(String recipientType, Mono<GetDigitalAddressINADRequestBodyDto> extractDigitalAddressINADRequestBodyDto, final ServerWebExchange exchange) {
        RecipientType recipientTypeEnum = RecipientType.fromString(recipientType);
        return extractDigitalAddressINADRequestBodyDto
                .flatMap(requestBody -> {
                    validateTaxIdUtils.validateTaxId(requestBody.getFilter().getTaxId(), PROCESS_NAME_INAD_ADDRESS, false);
                    RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(requestBody.getFilter().getCorrelationId(), exchange);
                    return MDCUtils.addMDCToContextAndExecute(inadService.callEService(requestBody)
                                    .map(responseRequestDigitalAddressDto ->
                                            InadConverter.mapToResponseOk(responseRequestDigitalAddressDto, recipientTypeEnum, requestBody.getFilter().getTaxId(), nationalRegistriesConfig.isEnablePfPecFallbackFlow(), requestBody.getFilter().getCorrelationId())))
                            .map(ResponseEntity::ok);
                });
    }
}
