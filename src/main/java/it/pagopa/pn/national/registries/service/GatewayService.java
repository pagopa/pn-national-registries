package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHECKING_CX_ID_FLAG;
import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;

@Service
@lombok.CustomLog
@RequiredArgsConstructor
public class GatewayService extends GatewayConverter {

    private final SqsService sqsService;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final PhysicalAddressService physicalAddressService;
    private final DigitalAddressService digitalAddressService;
    private final GatewayUtils gatewayUtils;

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddressAsync(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto request) {
        checkFlagPnNationalRegistriesCxId(pnNationalRegistriesCxId);
        String correlationId = request.getFilter().getCorrelationId();

        return sqsService.pushToInputQueue(InternalCodeSqsDto.builder()
                    .taxId(request.getFilter().getTaxId())
                    .correlationId(request.getFilter().getCorrelationId())
                    .recipientType(recipientType)
                    .domicileType(request.getFilter().getDomicileType().getValue())
                    .referenceRequestDate(request.getFilter().getReferenceRequestDate())
                    .pnNationalRegistriesCxId(pnNationalRegistriesCxId)
                    .build(), pnNationalRegistriesCxId)
                .thenReturn(mapToAddressesOKDto(correlationId));
    }

    public Mono<AddressOKDto> handleMessage(PnAddressGatewayEvent.Payload payload) {
        AddressRequestBodyDto addressRequestBodyDto = toAddressRequestBodyDto(payload);
        return retrieveDigitalOrPhysicalAddress(payload.getRecipientType(), payload.getPnNationalRegistriesCxId(), addressRequestBodyDto)
                .contextWrite(ctx -> gatewayUtils.enrichFluxContext(ctx, MDCUtils.retrieveMDCContextMap()));
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddress(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        log.info("recipientType {} and domicileType {}", recipientType, addressRequestBodyDto.getFilter().getDomicileType());
        RecipientType recipientTypeEnum = RecipientType.fromString(recipientType);
        return switch (recipientTypeEnum) {
            case PF -> retrieveAddressForPF(pnNationalRegistriesCxId, addressRequestBodyDto);
            case PG -> retrieveAddressForPG(pnNationalRegistriesCxId, addressRequestBodyDto);
        };
    }

    private Mono<AddressOKDto> retrieveAddressForPF(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        InternalCodeSqsDto dlqMessage = toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), PF.name(), pnNationalRegistriesCxId);

        Mono<Void> operation = isPhysical(addressRequestBodyDto)
                ? physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId)
                : getDigitalAddressForPF(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId);

        return operation
                .onErrorResume(e -> handleError(e, getOutputMessage(addressRequestBodyDto, correlationId, e), pnNationalRegistriesCxId, dlqMessage))
                .thenReturn(mapToAddressesOKDto(correlationId));
    }

    private Mono<Void> getDigitalAddressForPF(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return nationalRegistriesConfig.isEnablePfPecFallbackFlow()
                ? digitalAddressService.retrieveDigitalAddressForPFFromInadWithIniPecFallback(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId)
                : digitalAddressService.retrieveDigitalAddressFromInadForPF(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId);
    }

    private CodeSqsDto getOutputMessage(AddressRequestBodyDto addressRequestBodyDto, String correlationId, Throwable error) {
        if (isPhysical(addressRequestBodyDto)) {
            return errorAnprToSqsDto(correlationId, error);
        }
        return nationalRegistriesConfig.isEnablePfPecFallbackFlow() ? null : errorInadToSqsDto(correlationId, error);
    }

    private Mono<AddressOKDto> retrieveAddressForPG(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        InternalCodeSqsDto dlqMessage = toInternalCodeSqsDto(addressRequestBodyDto.getFilter(), PG.name(), pnNationalRegistriesCxId);

        Mono<Void> operation = isPhysical(addressRequestBodyDto)
                ? physicalAddressService.retrieveAsyncPhysicalAddressFromRegistroImprese(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId)
                : digitalAddressService.retrieveDigitalAddressForPG(pnNationalRegistriesCxId, addressRequestBodyDto, correlationId);

        return operation
                .onErrorResume(e -> handleError(e, null, pnNationalRegistriesCxId, dlqMessage))
                .thenReturn(mapToAddressesOKDto(correlationId));
    }

    private Mono<Void> handleError(Throwable error, CodeSqsDto outputMessage, String cxId, InternalCodeSqsDto dlqMessage) {
        return outputMessage != null ? sqsService.pushToOutputQueue(outputMessage, cxId).then() : handleExceptionAndSendToDlq(error, dlqMessage);
    }

    private boolean isPhysical(AddressRequestBodyDto request) {
        return AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(request.getFilter().getDomicileType());
    }


    private void checkFlagPnNationalRegistriesCxId(String pnNationalRegistriesCxId) {
        log.logChecking(PROCESS_CHECKING_CX_ID_FLAG);
        if (nationalRegistriesConfig.isValCxIdEnabled() && pnNationalRegistriesCxId == null) {
            log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG, false, "pnNationalRegistriesCxId required");
            throw new PnNationalRegistriesException("pnNationalRegistriesCxId required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
        log.logCheckingOutcome(PROCESS_CHECKING_CX_ID_FLAG, true);
    }

    public Mono<Void> handleExceptionAndSendToDlq(Throwable throwable, InternalCodeSqsDto internalCodeSqsDto) {
        if (isDlqEligible(throwable)){
            return sqsService.pushToInputDlqQueue(internalCodeSqsDto, internalCodeSqsDto.getPnNationalRegistriesCxId())
                    .doOnNext(sendMessageResponse -> log.info("Sent to DQL Input message for correlationId {} -> response: {}",
                            internalCodeSqsDto.getCorrelationId(),
                            sendMessageResponse))
                    .then();
        }
        return Mono.error(throwable);
    }

    private boolean isDlqEligible(Throwable throwable) {
        return throwable instanceof PnNationalRegistriesException exception && (exception.getStatusCode() == HttpStatus.BAD_REQUEST
                || exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
    }

    public Mono<PhysicalAddressesResponseDto> retrieveSyncPhysicalAddresses(PhysicalAddressesRequestBodyDto physicalAddressesRequestBodyDto) {
        return physicalAddressService.retrieveSyncPhysicalAddresses(physicalAddressesRequestBodyDto);
    }
}
