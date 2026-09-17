package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Objects;

import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfRequestedMetric;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetric;

@RequiredArgsConstructor
@Component
@CustomLog
public class PhysicalAddressService extends GatewayConverter {

    private static final String AUDIT_LOG_END_SUCCESS_MESSAGE = "The registry {} has responded successfully for the request with correlationId: {} and recIndex: {}";
    private static final String AUDIT_LOG_END_FAILURE_MESSAGE = "The registry {} has responded with an error for the request with correlationId: {} and recIndex: {}";

    private final InfoCamereService infoCamereService;
    private final AnprService anprService;
    private final GatewayUtils gatewayUtils;
    private final SqsService sqsService;

    public Mono<PhysicalAddressesResponseDto> retrieveSyncPhysicalAddresses(PhysicalAddressesRequestBodyDto request) {
        log.info("Starting retrievePhysicalAddresses - request: {}", request);
        if (CollectionUtils.isEmpty(request.getAddresses())) {
            return Mono.error(new PnNationalRegistriesException("addresses required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class));
        }

        return Flux.fromIterable(toAddressQueryRequests(request))
                .flatMap(this::retrieveSyncPhysicalAddress)
                .collectList()
                .map(addresses -> convertToPhysicalAddressesResponseDto(addresses, request.getCorrelationId()));
    }

    private Mono<PhysicalAddressResponseDto> retrieveSyncPhysicalAddress(AddressQueryRequest addressQueryRequest) {
        return switch (addressQueryRequest.getRecipientType()) {
            case PF -> retrieveSyncPhysicalAddressForPF(addressQueryRequest);
            case PG -> retrieveSyncPhysicalAddressForPG(addressQueryRequest);
        };
    }

    private Mono<PhysicalAddressResponseDto> retrieveSyncPhysicalAddressForPF(AddressQueryRequest addressQueryRequest) {
        PnAuditLogEvent auditLogEvent = gatewayUtils.buildAndPrintRequestAuditLog(addressQueryRequest, GatewayDownstreamService.ANPR);

        return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressQueryRequest))
                .doOnNext(getAddressANPROKDto -> logCfRequestedMetric(addressQueryRequest.getCorrelationId(), GatewayDownstreamService.ANPR, 1))
                .map(res -> convertAnprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .doOnNext(physicalAddressResponseDto -> {
                    if(Objects.nonNull(physicalAddressResponseDto.getPhysicalAddress())){
                        logCfWithAddressMetric(
                                addressQueryRequest.getCorrelationId(),
                                GatewayDownstreamService.ANPR,
                                RecipientType.PF,
                                null
                        );
                    }
                })
                .onErrorResume(isAnprAddressNotFound, e -> {
                    log.info("correlationId: {} recIndex {} - ANPR - indirizzo non presente", addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex());
                    logCfRequestedMetric(addressQueryRequest.getCorrelationId(), GatewayDownstreamService.ANPR, 1);
                    return Mono.just(anprNotFoundErrorToPhysicalAddressSQSMessage(addressQueryRequest));
                })
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log())
                .onErrorResume(t -> Mono.just(gatewayUtils.handleException(t, addressQueryRequest, GatewayDownstreamService.ANPR)));
    }

    private Mono<PhysicalAddressResponseDto> retrieveSyncPhysicalAddressForPG(AddressQueryRequest addressQueryRequest) {
        PnAuditLogEvent auditLogEvent = gatewayUtils.buildAndPrintRequestAuditLog(addressQueryRequest, GatewayDownstreamService.REGISTRO_IMPRESE);
        return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressQueryRequest))
                .doOnNext(getAddressRegistroImpreseOKDto -> logCfRequestedMetric(addressQueryRequest.getCorrelationId(), GatewayDownstreamService.REGISTRO_IMPRESE, 1))
                .map(res -> convertRegImprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .doOnNext(physicalAddressResponseDto -> {
                    if(Objects.nonNull(physicalAddressResponseDto.getPhysicalAddress())){
                        logCfWithAddressMetric(
                                addressQueryRequest.getCorrelationId(),
                                GatewayDownstreamService.REGISTRO_IMPRESE,
                                RecipientType.PG,
                                null
                        );
                    }
                })
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log())
                .onErrorResume(t -> Mono.just(gatewayUtils.handleException(t, addressQueryRequest, GatewayDownstreamService.REGISTRO_IMPRESE)));
    }

    public Mono<Void> retrieveAsyncPhysicalAddressFromAnpr(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                .doOnNext(getAddressANPROKDto -> logCfRequestedMetric(correlationId, GatewayDownstreamService.ANPR, 1))
                .map(getAddressANPROKDto -> anprToSqsDto(correlationId, getAddressANPROKDto))
                .flatMap(addressSQSMessageDto -> {
                    if(Objects.nonNull(addressSQSMessageDto.getPhysicalAddress())){
                        logCfWithAddressMetric(
                                correlationId,
                                GatewayDownstreamService.ANPR,
                                RecipientType.PF,
                                null
                        );
                    }
                    return sqsService.pushToOutputQueue(addressSQSMessageDto, pnNationalRegistriesCxId);
                })
                .doOnError(isAnprAddressNotFound, e -> {
                    log.info("correlationId: {} - ANPR - indirizzo non presente", correlationId);
                    logCfRequestedMetric(correlationId, GatewayDownstreamService.ANPR, 1);
                })
                .doOnNext(sendMessageResponse -> log.info("retrieved physycal address from ANPR for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId()))
                .then()
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve physical address from ANPR: {}"));
    }

    public Mono<Void> retrieveAsyncPhysicalAddressFromRegistroImprese(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                .doOnNext(getAddressRegistroImpreseOKDto -> logCfRequestedMetric(correlationId, GatewayDownstreamService.REGISTRO_IMPRESE, 1))
                .map(registroImpreseResponse -> regImpToSqsDto(correlationId, registroImpreseResponse))
                .flatMap(addressSQSMessageDto -> {
                    if(Objects.nonNull(addressSQSMessageDto.getPhysicalAddress())){
                        logCfWithAddressMetric(
                                correlationId,
                                GatewayDownstreamService.REGISTRO_IMPRESE,
                                RecipientType.PG,
                                null
                        );
                    }
                    return sqsService.pushToOutputQueue(addressSQSMessageDto, pnNationalRegistriesCxId);
                })
                .then()
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve physical address from Registro Imprese: {}"));
    }


}
