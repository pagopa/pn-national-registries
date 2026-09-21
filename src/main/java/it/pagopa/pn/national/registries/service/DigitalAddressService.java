package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.isValidEmail;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfRequestedMetric;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetric;

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
                .doOnNext(ipaPecDto -> logCfRequestedMetric(correlationId, GatewayDownstreamService.IPA, 1))
                .flatMap(response -> {
                    if ((response.getDomicilioDigitale() == null &&
                            response.getDenominazione() == null &&
                            response.getCodEnte() == null &&
                            response.getTipo() == null) ||
                            !isValidEmail(response.getDomicilioDigitale())) {
                        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto), addressRequestBodyDto.getFilter().getReferenceRequestDate(), PG)
                                .then();
                    }
                    log.info("retrieved digital address from IPA for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId());
                    logCfWithAddressMetric(
                            correlationId,
                            GatewayDownstreamService.IPA,
                            PG,
                            DigitalAddressRecipientType.IMPRESA
                    );
                    return sqsService.pushToOutputQueue(ipaToSqsDto(correlationId, response), pnNationalRegistriesCxId).then();
                })
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from IPA: {}"));
    }


    public Mono<Void> retrieveDigitalAddressFromInadForPF(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return callInadForPF(addressRequestBodyDto)
                .map(response -> inadToSqsDto(correlationId, response))
                .doOnNext(codeSqsDto -> {
                    if (!CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())) {
                        logCfWithAddressMetric(
                                correlationId,
                                GatewayDownstreamService.INAD,
                                PF,
                                DigitalAddressRecipientType.PERSONA_FISICA
                        );
                    }
                })
                .flatMap(codeSqsDto -> sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId))
                .then();
    }

    public Mono<Void> retrieveDigitalAddressForPFFromInadWithIniPecFallback(String pnNationalRegistriesCxId, AddressRequestBodyDto request, String correlationId) {
        return callInadForPF(request)
                .map(response -> inadToSqsDto(correlationId, response))
                .onErrorResume(e -> {
                    CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, e);
                    if(codeSqsDto != null) {
                        log.info("correlationId: {} - PEC not found on INAD, fallback to INI-PEC", correlationId);
                        return Mono.just(codeSqsDto);
                    }
                    return Mono.error(e);
                })
                .flatMap(codeSqsDto -> {
                    if (CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())) {
                        return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(request), request.getFilter().getReferenceRequestDate(), PF)
                                .then();
                    }
                    logCfWithAddressMetric(
                            correlationId,
                            GatewayDownstreamService.INAD,
                            PF,
                            DigitalAddressRecipientType.fromValue(codeSqsDto.getDigitalAddress().getFirst().getRecipient())
                    );
                    return sqsService.pushToOutputQueue(codeSqsDto, pnNationalRegistriesCxId).then();
                });
    }

    private Mono<GetDigitalAddressINADOKDto> callInadForPF(AddressRequestBodyDto request) {
        String correlationId = request.getFilter().getCorrelationId();
        return inadService.callEService(convertToGetDigitalAddressInadRequest(request), PF)
                .doOnNext(response -> logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1))
                .flatMap(DigitalAddressUtils::emailValidation)
                .doOnNext(response -> log.info("retrieved digital address from INAD for correlationId: {}", correlationId))
                .doOnError(isInadAddressNotFound, e -> {
                    log.info("correlationId: {} - INAD - indirizzo non presente", correlationId);
                    logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1);
                })
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from INAD: {}"));
    }
}
