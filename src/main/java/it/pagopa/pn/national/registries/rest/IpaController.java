package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.api.IpaApi;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.utils.RequestIdUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_IPA_ADDRESS;

@RestController
@lombok.CustomLog
@RequiredArgsConstructor
public class IpaController implements IpaApi {

    private final IpaService ipaService;
    private final ValidateTaxIdUtils validateTaxIdUtils;

    /**
     * POST /national-registries-private/ipa/pec : Il servizio consente di individuare i domicili digitali associati ad un codice fiscale di un Ente o al codice fiscale di un suo servizio di fatturazione elettronica.
     * Il servizio consente di individuare i domicili digitali associati ad un codice fiscale di un Ente o al codice fiscale di un suo servizio di fatturazione elettronica.
     *
     * @param ipARequestBodyDto (required)
     * @return OK (status code 200)
     * or Unauthorized (status code 401)
     * or Internal server error (status code 500)
     * or Service Unavailable (status code 503)
     */
    @Override
    public Mono<ResponseEntity<IPAPecDto>> ipaPec(Mono<IPARequestBodyDto> ipARequestBodyDto, ServerWebExchange exchange) {
        return ipARequestBodyDto
                .flatMap(requestBody -> {
                    validateTaxIdUtils.validateTaxId(requestBody.getFilter().getTaxId(), PROCESS_NAME_IPA_ADDRESS, false);
                    RequestIdUtils.putRequestIdToMDCAndExchangeResponseHeader(requestBody.getFilter().getCorrelationId(), exchange);
                    return MDCUtils.addMDCToContextAndExecute(ipaService.getIpaPec(requestBody))
                            .map(ResponseEntity::ok);
                });
    }
}
