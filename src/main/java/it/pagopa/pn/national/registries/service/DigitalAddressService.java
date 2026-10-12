package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.model.AddressInternalResult;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.utils.DigitalAddressUtils;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.isValidEmail;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfRequestedMetric;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetric;

@Component
@RequiredArgsConstructor
@CustomLog
public class DigitalAddressService extends GatewayConverter {

    private final IpaService ipaService;
    private final InadService inadService;
    private final GatewayUtils gatewayUtils;

    Mono<AddressInternalResult> retrieveDigitalAddressFromIpa(AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return ipaService.getIpaPec(convertToGetIpaPecRequest(addressRequestBodyDto))
                .doOnNext(ipaPecDto -> logCfRequestedMetric(correlationId, GatewayDownstreamService.IPA, 1))
                .<AddressInternalResult>map(response -> {
                    if ((response.getDomicilioDigitale() == null &&
                            response.getDenominazione() == null &&
                            response.getCodEnte() == null &&
                            response.getTipo() == null) ||
                            !isValidEmail(response.getDomicilioDigitale())) {
                        return new AddressInternalResult.NotFound();

                    }
                    log.info("retrieved digital address from IPA for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId());
                    logCfWithAddressMetric(
                            correlationId,
                            GatewayDownstreamService.IPA,
                            PG,
                            DigitalAddressRecipientType.IMPRESA
                    );
                    return new AddressInternalResult.Found(ipaToSqsDto(correlationId, response));
                })
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from IPA: {}"));
    }

    public Mono<AddressInternalResult> retrieveDigitalAddressFromInadSync(AddressRequestBodyDto addressRequestBodyDto, String correlationId, RecipientType recipientType) {
        return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto), recipientType, correlationId)
                .doOnNext(response -> logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1))
                .flatMap(DigitalAddressUtils::emailValidation)
                .doOnNext(response -> log.info("retrieved digital address from INAD for correlationId: {}", correlationId))
                .doOnError(isInadAddressNotFound, e -> {
                    log.info("correlationId: {} - INAD - indirizzo non presente", correlationId);
                    logCfRequestedMetric(correlationId, GatewayDownstreamService.INAD, 1);
                })
                .map(response -> inadToSqsDto(correlationId, response))
                .<AddressInternalResult>map(codeSqsDto -> {
                    if (!CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress())) {
                        logCfWithAddressMetric(correlationId, GatewayDownstreamService.INAD, recipientType, DigitalAddressRecipientType.fromValue(codeSqsDto.getDigitalAddress().getFirst().getRecipient()));
                        return new AddressInternalResult.Found(codeSqsDto);
                    }

                    return new AddressInternalResult.NotFound(codeSqsDto);
                })
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve digital address from INAD: {}"));
    }
}