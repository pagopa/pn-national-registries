package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@CustomLog
public class PhysicalAddressService extends GatewayConverter {

    private static final String AUDIT_LOG_END_SUCCESS_MESSAGE = "The registry {} has responded successfully for the request with correlationId: {} and recIndex: {}";
    private static final String AUDIT_LOG_END_FAILURE_MESSAGE = "The registry {} has responded with an error for the request with correlationId: {} and recIndex: {}";
    public static final Pattern ANPR_CF_NOT_FOUND = Pattern.compile("(\"codiceErroreAnomalia\")\\s*:\\s*\"(EN122)\"", Pattern.CASE_INSENSITIVE);

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
                .map(res -> convertAnprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .onErrorResume(isAnprAddressNotFound, e -> {
                    log.info("correlationId: {} recIndex {} - ANPR - indirizzo non presente", addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex());
                    return Mono.just(anprNotFoundErrorToPhysicalAddressSQSMessage(addressQueryRequest));
                })
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.ANPR, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log())
                .onErrorResume(t -> Mono.just(gatewayUtils.handleException(t, addressQueryRequest, GatewayDownstreamService.ANPR)));
    }

    public final Predicate<Throwable> isAnprAddressNotFound = t -> t instanceof PnNationalRegistriesException exception
            && exception.getStatusCode() == HttpStatus.NOT_FOUND
            && StringUtils.hasText(exception.getResponseBodyAsString())
            && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find();

    private Mono<PhysicalAddressResponseDto> retrieveSyncPhysicalAddressForPG(AddressQueryRequest addressQueryRequest) {
        PnAuditLogEvent auditLogEvent = gatewayUtils.buildAndPrintRequestAuditLog(addressQueryRequest, GatewayDownstreamService.REGISTRO_IMPRESE);
        return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressQueryRequest))
                .map(res -> convertRegImprResponseToInternalRecipientAddress(res, addressQueryRequest))
                .doOnNext(res -> auditLogEvent.generateSuccess(AUDIT_LOG_END_SUCCESS_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex()).log())
                .doOnError(e -> auditLogEvent.generateFailure(AUDIT_LOG_END_FAILURE_MESSAGE, GatewayDownstreamService.REGISTRO_IMPRESE, addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex(), e).log())
                .onErrorResume(t -> Mono.just(gatewayUtils.handleException(t, addressQueryRequest, GatewayDownstreamService.REGISTRO_IMPRESE)));
    }

    public Mono<Void> retrieveAsyncPhysicalAddressFromAnpr(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                .flatMap(anprResponse -> sqsService.pushToOutputQueue(anprToSqsDto(correlationId, anprResponse), pnNationalRegistriesCxId))
                .doOnNext(sendMessageResponse -> log.info("retrieved physycal address from ANPR for correlationId: {}", addressRequestBodyDto.getFilter().getCorrelationId()))
                .then()
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve physical address from ANPR: {}"));
    }

    public Mono<Void> retrieveAsyncPhysicalAddressFromRegistroImprese(String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto, String correlationId) {
        return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                .flatMap(registroImpreseResponse -> sqsService.pushToOutputQueue(regImpToSqsDto(correlationId, registroImpreseResponse), pnNationalRegistriesCxId))
                .then()
                .doOnError(e -> gatewayUtils.logEServiceError(e, "can not retrieve physical address from Registro Imprese: {}"));
    }


}
