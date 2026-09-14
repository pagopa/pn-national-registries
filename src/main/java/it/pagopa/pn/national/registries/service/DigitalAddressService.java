package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.isValidEmail;

@Component
@RequiredArgsConstructor
@CustomLog
public class DigitalAddressService extends GatewayConverter {

    private final IpaService ipaService;
    private final InfoCamereService infoCamereService;
    private final InadService inadService;
    private final SqsService sqsService;
    private final GatewayUtils gatewayUtils;

    Mono<Void> retrieveDigitalAddressForPG(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return ipaService.getIpaPec(convertToGetIpaPecRequest(addressRequestBodyDto))
                .flatMap(response -> {
                    if ((response.getDomicilioDigitale() == null &&
                            response.getDenominazione() == null &&
                            response.getCodEnte() == null &&
                            response.getTipo() == null) ||
                            !isValidEmail(response.getDomicilioDigitale())) {
                        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto), addressRequestBodyDto.getFilter().getReferenceRequestDate())
                                .then();
                    }
                    log.info("retrieved digital address from IPA for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId());
                    return sqsService.pushToOutputQueue(ipaToSqsDto(correlationId, response), pnNationalRegistriesCxId).then();
                })
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from IPA: {}"));
    }


    public Mono<Void> retrieveDigitalAddressFromInad(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return callInad(addressRequestBodyDto)
                .map(response -> inadToSqsDto(correlationId, response, DigitalAddressRecipientType.PERSONA_FISICA))
                .flatMap(codeSqsDto -> sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId))
                .onErrorResume(e -> {
                    AddressSQSMessageDto codeSqsDto = errorInadToSqsDto(correlationId, e);
                    if(codeSqsDto != null) {
                        return sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId);
                    }
                    return Mono.error(e);
                })
                .then();
    }

    public Mono<Void> retrieveDigitalAddressWithIniPecFallback(String pnNationalRegistriesCxId, AddressRequestBodyDto request, String correlationId) {
        return callInad(request)
                .map(response -> inadToSqsDto(correlationId, response, DigitalAddressRecipientType.PERSONA_FISICA))
                .onErrorResume(e -> {
                    if (isInadNotFound(e)) {
                        log.info("correlationId: {} - PEC not found on INAD, fallback to INI-PEC", correlationId);
                        return Mono.just(emptyDigitalAddressSqsDto(correlationId));
                    }
                    return Mono.error(e);
                })
                .flatMap(codeSqsDto -> {
                    if (CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())) {
                        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(request), request.getFilter().getReferenceRequestDate())
                                .then();
                    }
                    return sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId).then();
                });
    }

    public boolean isInadNotFound(Throwable e) {
        return e instanceof PnNationalRegistriesException pnNationalRegistriesException && pnNationalRegistriesException.getStatusCode().equals(HttpStatusCode.valueOf(404));
    }

    private Mono<GetDigitalAddressINADOKDto> callInad(AddressRequestBodyDto request) {
        String correlationId = request.getFilter().getCorrelationId();
        return inadService.callEService(convertToGetDigitalAddressInadRequest(request), PF)
                .flatMap(DigitalAddressUtils::emailValidation)
                .doOnNext(response -> log.info("retrieved digital address from INAD for correlationId: {}", correlationId))
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from INAD: {}"));
    }
}
